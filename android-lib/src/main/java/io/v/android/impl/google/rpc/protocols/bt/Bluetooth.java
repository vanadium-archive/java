// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.impl.google.rpc.protocols.bt;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;

import org.joda.time.Duration;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import io.v.v23.verror.VException;

/**
 * Handles bluetooth connection establishment on Android.
 * <p>
 * Used as a helper class for native code which sets up and registers the bluetooth protocol with
 * the vanadium RPC service.
 */
class Bluetooth {
    private static final List<Integer> BLUETOOTH_PORTS = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9,
            10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31);

    static Listener listen(String btAddr) throws VException {
        String macAddr = getMACAddress(btAddr);
        int port = getPortNumber(btAddr);
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (!macAddr.equals(adapter.getAddress())) {
            throw new VException("Illegal MAC address to listen on: no local device found with "
                    + "MAC address: " + macAddr + " (local address is: " + adapter.getAddress()
                    + " )");
        }
        List<Integer> ports = null;
        if (port == 0) {  // listen on the first available port.
            ports = new ArrayList(BLUETOOTH_PORTS);
            Collections.shuffle(ports);
        } else {  // listen on a specific port only
            ports = ImmutableList.of(port);
        }
        VException lastError = null;
        for (int portNum : ports) {
            try {
                BluetoothServerSocket socket = listenOnPort(portNum);
                return new Listener(socket, String.format("%s/%d", macAddr, portNum));
            } catch (VException e) {
                // OK, try the next one
                lastError = e;
            }
        }
        throw lastError;
    }

    static Connection dial(String btAddr, Duration timeout) throws VException {
        String macAddr = getMACAddress(btAddr);
        int port = getPortNumber(btAddr);
        BluetoothDevice device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(macAddr);
        try {
            // Create a socket to the remote device.
            // NOTE(spetrovic): Android's public methods currently only allow connection to a
            // UUID, which goes through SDP.  Since we already have a remote port number, we
            // connect to it directly.
            Method m = device.getClass().getMethod("createInsecureRfcommSocket",
                    new Class[]{int.class});
            final BluetoothSocket socket = (BluetoothSocket) m.invoke(device, port);
            // Connect.
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        System.err.println("Couldn't close BluetoothSocket.");
                    }
                }
            }, timeout.getMillis());
            try {
                socket.connect();
            } catch (IOException e) {
                throw new VException("Couldn't connect: " + e.getMessage());
            } finally {
                timer.cancel();
            }
            // There is no way currently to retrieve the local port number for the connection,
            // but that's probably OK.
            String localAddr = String.format("%s/%d",
                    BluetoothAdapter.getDefaultAdapter().getAddress(), 0);
            String remoteAddr = String.format("%s/%d", macAddr, port);
            return new Connection(socket, localAddr, remoteAddr);
        } catch (Exception e) {
            throw new VException("Couldn't invoke createInsecureRfcommSocket: "
                    + e.getMessage());
        }
    }

    private static BluetoothServerSocket listenOnPort(int port) throws VException {
        // Use reflection to reach the hidden "listenUsingInsecureRfcommOn(port)" method.
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        try {
            Method m = adapter.getClass().
                    getMethod("listenUsingInsecureRfcommOn", new Class[]{int.class});
            return (BluetoothServerSocket) m.invoke(adapter, port);
        } catch (Exception e) {
            throw new VException("Error invoking listenUsingInsecureRfcommOn: "
                    + e.getMessage());
        }
    }

    private static String getMACAddress(String btAddr) throws VException {
        List<String> parts = Splitter.on("/").omitEmptyStrings().splitToList(btAddr);
        switch (parts.size()) {
            case 0:
                throw new VException(String.format(
                        "Couldn't split bluetooth address \"%s\" using \"/\" separator: " +
                                "got zero parts!", btAddr));
            case 1:
                return BluetoothAdapter.getDefaultAdapter().getAddress();
            case 2:
                String address = parts.get(0).toUpperCase();
                if (!BluetoothAdapter.checkBluetoothAddress(address)) {
                    throw new VException("Invalid bluetooth address: " + btAddr);
                }
                return address;
            default:
                throw new VException(String.format(
                        "Couldn't parse bluetooth address \"%s\": too many \"/\".", btAddr));
        }
    }

    private static int getPortNumber(String btAddr) throws VException {
        List<String> parts = Splitter.on("/").splitToList(btAddr);
        switch (parts.size()) {
            case 0:
                throw new VException(String.format(
                        "Couldn't split bluetooth address \"%s\" using \"/\" separator: " +
                                "got zero parts!", btAddr));
            case 1:
            case 2:
                try {
                    int port = Integer.parseInt((parts.get(parts.size() - 1)));
                    if (port < 0 || port > 32) {
                        throw new VException(String.format("Illegal port number %q in bluetooth " +
                                "address \"%s\".", port, btAddr));
                    }
                    return port;
                } catch (NumberFormatException e) {
                    throw new VException(String.format(
                            "Couldn't parse port number in bluetooth address \"%s\": %s",
                            btAddr, e.getMessage()));
                }
            default:
                throw new VException(String.format(
                        "Couldn't parse bluetooth address \"%s\": too many \"/\".", btAddr));
        }
    }

    static class Listener {
        private final BluetoothServerSocket serverSocket;
        private final String localAddress;

        Listener(BluetoothServerSocket serverSocket, String address) {
            this.serverSocket = serverSocket;
            this.localAddress = address;
        }

        Connection accept() throws IOException {
            BluetoothSocket socket = serverSocket.accept();
            // There is no way currently to retrieve the remote end's channel number, but that's
            // probably OK.
            String remoteAddress = String.format("%s/%d", socket.getRemoteDevice().getAddress(), 0);
            return new Connection(socket, localAddress, remoteAddress);
        }

        void close() throws IOException {
            serverSocket.close();
        }

        String address() {
            return localAddress;
        }
    }

    static class Connection {
        private final BluetoothSocket socket;
        private final String localAddress;
        private final String remoteAddress;
        private final byte[] buf = new byte[8096];

        Connection(BluetoothSocket socket, String localAddress, String remoteAddress) {
            this.socket = socket;
            this.localAddress = localAddress;
            this.remoteAddress = remoteAddress;
        }

        synchronized byte[] read() throws IOException {
            int num = socket.getInputStream().read(buf);
            return num == buf.length ? buf : Arrays.copyOf(buf, num);
        }

        synchronized void write(byte[] data) throws IOException {
            socket.getOutputStream().write(data);
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
