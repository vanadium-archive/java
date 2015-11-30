// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.libs.discovery.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;
import android.support.v4.content.ContextCompat;
import android.Manifest;

import org.joda.time.Duration;

import com.google.common.util.concurrent.Uninterruptibles;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import io.v.impl.google.lib.discovery.DeviceCache;
import io.v.impl.google.lib.discovery.UUIDUtil;
import io.v.impl.google.lib.discovery.VScanner;
import io.v.impl.google.lib.discovery.ble.BleAdvertisementConverter;
import io.v.v23.context.VContext;
import io.v.impl.google.lib.discovery.ScanHandler;
import io.v.x.ref.lib.discovery.Advertisement;

/**
 * The discovery plugin interface for Bluetooth.
 */
public class BlePlugin {
    // We are using a constant for the MTU because Android and paypal/gatt don't get along
    // when the paypal gatt client sends a setMTU message.  The Android server seems to send
    // a malformed L2CAP message.
    private static final int MTU = 23;

    // Object used to lock advertisement objects.
    private final Object advertisementLock = new Object();
    // The id to assign to the next advertisment.
    private int nextAdv;
    // A map of advertisement ids to the advertisement that corresponds to them.
    private final Map<Integer, BluetoothGattService> advertisements = new HashMap<>();
    // A map of advertisement ids to the thread waiting for cancellation of the context.
    private final Map<Integer, Thread> advCancellationThreads = new HashMap<>();

    // Object used to lock scanner objects
    private final Object scannerLock = new Object();
    // A map of scanner ids to the thread waiting for cancellation of the context.
    private final Map<Integer, Thread> scanCancellationThreads = new HashMap<>();
    private final DeviceCache cachedDevices;
    // Used to track the set of devices we currently talking to.
    private final Set<String> pendingCalls = new HashSet<>();

    // Set of Ble objects that will be interacted with to perform operations.
    private BluetoothLeAdvertiser bluetoothLeAdvertise;
    private BluetoothLeScanner bluetoothLeScanner;
    private BluetoothGattServer bluetoothGattServer;

    // We need to hold onto the callbacks for scan an advertise because that is what is used
    // to stop the operation.
    private ScanCallback scanCallback;
    private AdvertiseCallback advertiseCallback;

    private boolean isScanning;

    private final Context androidContext;

    // If isEnabled is false, then all operations on the ble plugin are no-oped.  This wil only
    // be false if the ble hardware is inaccessible.
    private boolean isEnabled = false;

    // A thread to wait for the cancellation of a particular advertisement.  VContext.done().await()
    // is blocking so have to spin up a thread per outstanding advertisement.
    private class AdvertisementCancellationRunner implements Runnable{
        private final VContext ctx;

        private final int id;
        AdvertisementCancellationRunner(VContext ctx, int id) {
            this.id = id;
            this.ctx = ctx;
        }

        @Override
        public void run() {
            Uninterruptibles.awaitUninterruptibly(ctx.done());
            BlePlugin.this.removeAdvertisement(id);
        }
    }

    // Similar to AdvertisementCancellationRunner except for scanning.
    private class ScannerCancellationRunner implements Runnable{
        private VContext ctx;

        private int id;
        ScannerCancellationRunner(VContext ctx, int id) {
            this.id = id;
            this.ctx = ctx;
        }

        @Override
        public void run() {
            Uninterruptibles.awaitUninterruptibly(ctx.done());
            BlePlugin.this.removeScanner(id);
        }
    }

    private boolean hasPermission(String perm) {
        return ContextCompat.checkSelfPermission(androidContext, perm) ==
                PackageManager.PERMISSION_GRANTED;
    }
    public BlePlugin(Context androidContext) {
        this.androidContext = androidContext;
        cachedDevices = new DeviceCache(Duration.standardMinutes(1));
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            return;
        }

        if (!hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION) &&
                !hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            return;
        }
        isEnabled = true;
        bluetoothLeAdvertise = BluetoothAdapter.getDefaultAdapter().getBluetoothLeAdvertiser();
        bluetoothLeScanner = BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner();
        BluetoothManager manager = (BluetoothManager) androidContext.getSystemService(
                Context.BLUETOOTH_SERVICE);
        bluetoothGattServer = manager.openGattServer(androidContext,
                new BluetoothGattServerCallback() {
            @Override
            public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
                super.onConnectionStateChange(device, status, newState);
            }

            @Override
            public void onCharacteristicReadRequest(BluetoothDevice device, int requestId,
                                                    int offset,
                                                    BluetoothGattCharacteristic characteristic) {
                super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
                byte[] total =characteristic.getValue();
                byte[] res = {};
                // Only send MTU - 1 bytes. The first byte of all packets is the op code.
                if (offset < total.length) {
                    int finalByte = offset + MTU - 1;
                    if (finalByte > total.length) {
                        finalByte = total.length;
                    }
                    res = Arrays.copyOfRange(total, offset, finalByte);
                    bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, res);
                } else {
                    // This should probably be an error, but a bug in the paypal/gatt code causes an
                    // infinite loop if this returns an error rather than the empty value.
                    bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0,  res);
                }
            }
        });
    }


    // Converts a Vanadium Advertisement to a Bluetooth gatt service.
    private BluetoothGattService convertToService(Advertisement adv) throws IOException {
        Map<UUID, byte[]> attributes = BleAdvertisementConverter.vAdvertismentToBleAttr(adv);
        BluetoothGattService service = new BluetoothGattService(
                UUIDUtil.UuidToUUID(adv.getServiceUuid()), BluetoothGattService.SERVICE_TYPE_PRIMARY);
        for (Map.Entry<UUID, byte[]> entry : attributes.entrySet()) {
            BluetoothGattCharacteristic ch = new BluetoothGattCharacteristic(entry.getKey(), 0,
                    BluetoothGattCharacteristic.PERMISSION_READ);
            ch.setValue(entry.getValue());
            service.addCharacteristic(ch);
        }
        return service;
    }

    public void addAdvertisement(VContext ctx, Advertisement advertisement) throws IOException {
        if (!isEnabled) {
            return;
        }
        BluetoothGattService service = convertToService(advertisement);
        synchronized (advertisementLock) {
            int currentId = nextAdv++;
            advertisements.put(currentId, service);
            Thread t = new Thread(new AdvertisementCancellationRunner(ctx, currentId));
            t.start();
            advCancellationThreads.put(currentId, t);
            bluetoothGattServer.addService(service);
            readvertise();
        }
    }

    private void removeAdvertisement(int id) {
        synchronized (advertisements) {
            BluetoothGattService s = advertisements.get(id);
            if (s != null) {
                bluetoothGattServer.removeService(s);
            }
            advertisements.remove(id);
            advCancellationThreads.remove(id);
            readvertise();
        }
    }

    public void addScanner(VContext ctx, UUID serviceUUID,  ScanHandler handler) {
        if (!isEnabled) {
            return;
        }
        VScanner scanner = new VScanner(serviceUUID, handler);
        int currentId = cachedDevices.addScanner(scanner);
        synchronized (scannerLock) {
            Thread t = new Thread(new ScannerCancellationRunner(ctx, currentId));
            t.start();
            scanCancellationThreads.put(currentId, t);
            updateScanning();
        }
    }

    private void removeScanner(int id) {
        cachedDevices.removeScanner(id);
        synchronized (scannerLock) {
            scanCancellationThreads.remove(id);
            updateScanning();
        }
    }

    private void updateScanning() {
        if (isScanning && scanCancellationThreads.size() == 0) {
            isScanning = false;
            bluetoothLeScanner.stopScan(scanCallback);
            return;
        }

        if (!isScanning && scanCancellationThreads.size() > 0) {
            isScanning = true;
            ScanFilter.Builder builder = new ScanFilter.Builder();
            byte[] manufacturerData = {};
            byte[] manufacturerMask = {};

            builder.setManufacturerData(1001, manufacturerData, manufacturerMask);
            final List<ScanFilter> scanFilter = new ArrayList<>();
            scanFilter.add(builder.build());


            scanCallback = new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    // in L the only value for callbackType is CALLBACK_TYPE_ALL_MATCHES, so
                    // we don't look at its value.
                    ScanRecord record = result.getScanRecord();
                    // Use 1001 to denote that this is a Vanadium device.  We picked an id that is
                    // currently not in use.
                    byte[] data = record.getManufacturerSpecificData(1001);
                    ByteBuffer buffer = ByteBuffer.wrap(data);
                    final long hash = buffer.getLong();
                    final String deviceId = result.getDevice().getAddress();
                    if (cachedDevices.haveSeenHash(hash, deviceId)) {
                        return;
                    }
                    synchronized (scannerLock) {
                        if (pendingCalls.contains(deviceId)) {
                            Log.d("vanadium", "not connecting to " + deviceId + " because of pending connection");
                            return;
                        }
                        pendingCalls.add(deviceId);
                    }
                    BluetoothGattClientCallback.Callback ccb = new BluetoothGattClientCallback.Callback() {
                        @Override
                        public void handle(Map<UUID, Map<UUID, byte[]>> services) {
                            Set<Advertisement> advs = new HashSet<>();
                            for (Map.Entry<UUID, Map<UUID, byte[]>> entry : services.entrySet()) {
                                try {
                                    Advertisement adv =
                                            BleAdvertisementConverter.
                                                    bleAttrToVAdvertisement(entry.getValue());
                                    advs.add(adv);
                                } catch (IOException e) {
                                    Log.e("vanadium","Failed to convert advertisement" + e);
                                }
                            }
                            cachedDevices.saveDevice(hash, advs, deviceId);
                            synchronized (scannerLock) {
                                pendingCalls.remove(deviceId);
                            }
                            bluetoothLeScanner.startScan(scanFilter, new ScanSettings.Builder().
                                    setScanMode(ScanSettings.SCAN_MODE_BALANCED).build(), scanCallback);
                        }
                    };
                    BluetoothGattClientCallback cb = new BluetoothGattClientCallback(ccb);
                    bluetoothLeScanner.stopScan(scanCallback);
                    Log.d("vanadium", "connecting to " + result.getDevice());
                    result.getDevice().connectGatt(androidContext, false, cb);
                }

                @Override
                public void onBatchScanResults(List<ScanResult> results) {
                }

                @Override
                public void onScanFailed(int errorCode) {
                }
            };
            bluetoothLeScanner.startScan(scanFilter, new ScanSettings.Builder().
                    setScanMode(ScanSettings.SCAN_MODE_BALANCED).build(), scanCallback);
        }
    }

    private void readvertise() {
        if (advertiseCallback != null) {
            bluetoothLeAdvertise.stopAdvertising(advertiseCallback);
            advertiseCallback = null;
        }
        if (advertisements.size() == 0) {
            return;
        }

        int hash = advertisements.hashCode();

        AdvertiseData.Builder builder = new AdvertiseData.Builder();
        ByteBuffer buf = ByteBuffer.allocate(9);
        buf.put((byte) 8);
        buf.putLong(hash);
        builder.addManufacturerData(1001, buf.array());
        AdvertiseSettings.Builder settingsBuilder = new AdvertiseSettings.Builder();
        settingsBuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY);
        settingsBuilder.setConnectable(true);
        advertiseCallback = new AdvertiseCallback() {
                    @Override
                    public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                        Log.i("vanadium", "Successfully started " + settingsInEffect);
                    }

                    @Override
                    public void onStartFailure(int errorCode) {
                        Log.i("vanadium", "Failed to start advertising " + errorCode);
                    }
                };
        bluetoothLeAdvertise.startAdvertising(settingsBuilder.build(), builder.build(),
                advertiseCallback);
    }
}