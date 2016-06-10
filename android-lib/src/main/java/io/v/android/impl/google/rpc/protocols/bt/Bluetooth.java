// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.impl.google.rpc.protocols.bt;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import com.google.common.base.Splitter;

import org.joda.time.Duration;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import io.v.android.v23.V;
import io.v.v23.context.VContext;
import io.v.v23.verror.VException;

/**
 * Handles bluetooth connection establishment on Android.
 * <p>
 * Used as a helper class for native code which sets up and registers the bluetooth protocol with
 * the vanadium RPC service.
 */
class Bluetooth {
    private static final String TAG = "Bluetooth";

    static Listener listen(VContext ctx, String btAddr) throws Exception {
        String macAddr = getMACAddress(ctx, btAddr);
        int port = getPortNumber(btAddr);
        BluetoothServerSocket socket = listenOnPort(port);
        if (port == 0) {
            // listen on the first available port. Get the port number.
            port = getPortNumber(socket);
        }
        Log.d(TAG, String.format("listening on port %d", port));
        return new Listener(socket, String.format("%s/%d", macAddr, port));
    }

    static Stream dial(VContext ctx, String btAddr, Duration timeout) throws Exception {
        String macAddr = getMACAddress(ctx, btAddr);
        int port = getPortNumber(btAddr);
        BluetoothDevice device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(macAddr);

        // Create a socket to the remote device.
        // NOTE(spetrovic): Android's public methods currently only allow connection to
        // a UUID, which goes through SDP.  Since we already have a remote port number,
        // we connect to it directly, invoking a hidden method using reflection.
        Method m =
                device.getClass().getMethod("createInsecureRfcommSocket", new Class[] {int.class});
        final BluetoothSocket socket = (BluetoothSocket) m.invoke(device, port);
        // Connect.
        Timer timer = null;
        if (timeout.getMillis() != 0) {
            timer = new Timer();
            timer.schedule(
                    new TimerTask() {
                        @Override
                        public void run() {
                            try {
                                socket.close();
                            } catch (IOException e) {
                            }
                        }
                    },
                    timeout.getMillis());
        }
        try {
            socket.connect();
        } catch (IOException e) {
            socket.close();
            throw e;
        } finally {
            if (timer != null) {
                timer.cancel();
            }
        }
        // There is no way currently to retrieve the local port number for the
        // connection, but that's probably OK.
        String localAddr = String.format("%s/%d", localMACAddress(ctx), 0);
        String remoteAddr = String.format("%s/%d", macAddr, port);
        return new Stream(socket, localAddr, remoteAddr);
    }

    private static BluetoothServerSocket listenOnPort(int port) throws Exception {
        //  Note that Android developer guide says that unlike TCP/IP, RFCOMM only allows
        //  one connected client per channel at a time:
        //  https://developer.android.com/guide/topics/connectivity/bluetooth.html.
        //  But this seems to be conflict with the android reference page.
        //  https://developer.android.com/reference/android/bluetooth/BluetoothServerSocket.html#accept()
        //
        //  Multiple client connection on a same listening channel seem to work with some testing devices
        //  like Nexus 6 or Nexus 9, but this is not guaranteed to work with other devices.
        if (port == 0) {
            // Use SOCKET_CHANNEL_AUTO_STATIC (-2) to auto assign a channel number.
            port = -2;
        }
        // Use reflection to reach the hidden "listenUsingInsecureRfcommOn(port)" method.
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        Method m =
                adapter.getClass()
                        .getMethod("listenUsingInsecureRfcommOn", new Class[] {int.class});
        return (BluetoothServerSocket) m.invoke(adapter, port);
    }

    private static int getPortNumber(BluetoothServerSocket serverSocket) throws Exception {
        // Use reflection to reach the hidden "getChannel()" method.
        Method m = serverSocket.getClass().getMethod("getChannel", new Class[0]);
        return (int) m.invoke(serverSocket);
    }

    private static String localMACAddress(VContext ctx) {
        // TODO(suharshs): Android has disallowed getting the local address.
        // This is a remaining working hack that gets the local bluetooth address,
        // just to get things working.
        return android.provider.Settings.Secure.getString(
                V.getAndroidContext(ctx).getContentResolver(), "bluetooth_address");
    }

    private static String getMACAddress(VContext ctx, String btAddr) throws VException {
        List<String> parts = Splitter.on("/").omitEmptyStrings().splitToList(btAddr);
        switch (parts.size()) {
            case 0:
                throw new VException(
                        String.format(
                                "Couldn't split bluetooth address \"%s\" using \"/\" separator: "
                                        + "got zero parts!",
                                btAddr));
            case 1:
                return localMACAddress(ctx);
            case 2:
                String address = parts.get(0).toUpperCase();
                if (!BluetoothAdapter.checkBluetoothAddress(address)) {
                    throw new VException("Invalid bluetooth address: " + btAddr);
                }
                return address;
            default:
                throw new VException(
                        String.format(
                                "Couldn't parse bluetooth address \"%s\": too many \"/\".",
                                btAddr));
        }
    }

    private static int getPortNumber(String btAddr) throws VException {
        List<String> parts = Splitter.on("/").splitToList(btAddr);
        switch (parts.size()) {
            case 0:
                throw new VException(
                        String.format(
                                "Couldn't split bluetooth address \"%s\" using \"/\" separator: "
                                        + "got zero parts!",
                                btAddr));
            case 1:
            case 2:
                try {
                    int port = Integer.parseInt((parts.get(parts.size() - 1)));
                    if (port < 0 || port > 30) {
                        throw new VException(
                                String.format(
                                        "Illegal port number %q in bluetooth " + "address \"%s\".",
                                        port, btAddr));
                    }
                    return port;
                } catch (NumberFormatException e) {
                    throw new VException(
                            String.format(
                                    "Couldn't parse port number in bluetooth address \"%s\": %s",
                                    btAddr, e.getMessage()));
                }
            default:
                throw new VException(
                        String.format(
                                "Couldn't parse bluetooth address \"%s\": too many \"/\".",
                                btAddr));
        }
    }

    static class Listener {
        private final BluetoothServerSocket serverSocket;
        private final String localAddress;

        Listener(BluetoothServerSocket serverSocket, String address) {
            this.serverSocket = serverSocket;
            this.localAddress = address;
        }

        Stream accept() throws IOException {
            try {
                BluetoothSocket socket = serverSocket.accept();
                // There is no way currently to retrieve the remote end's channel number,
                // but that's probably OK.
                String remoteAddress =
                        String.format("%s/%d", socket.getRemoteDevice().getAddress(), 0);
                return new Stream(socket, localAddress, remoteAddress);
            } catch (IOException e) {
                serverSocket.close();
                throw e;
            }
        }

        void close() throws IOException {
            serverSocket.close();
        }

        String address() {
            return localAddress;
        }
    }

    static class Stream {
        private final BluetoothSocket socket;
        private final String localAddress;
        private final String remoteAddress;

        Stream(BluetoothSocket socket, String localAddress, String remoteAddress) {
            this.socket = socket;
            this.localAddress = localAddress;
            this.remoteAddress = remoteAddress;
        }

        byte[] read(int n) throws IOException {
            try {
                InputStream in = socket.getInputStream();
                byte[] buf = new byte[n];
                int total = 0;
                while (total < n) {
                    int r = in.read(buf, total, n - total);
                    if (r < 0) {
                        break;
                    }
                    total += r;
                }
                return total == n ? buf : Arrays.copyOf(buf, total);
            } catch (IOException e) {
                socket.close();
                throw e;
            }
        }

        void write(byte[] data) throws IOException {
            try {
                OutputStream out = socket.getOutputStream();
                out.write(data);
                // TODO(jhahn): Do we need to flush for every write?
                // out.flush();
            } catch (IOException e) {
                socket.close();
                throw e;
            }
        }

        void close() throws IOException {
            socket.close();
        }

        String localAddress() {
            return this.localAddress;
        }

        String remoteAddress() {
            return this.remoteAddress;
        }
    }

    private Bluetooth() {}
}
