// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.impl.google.discovery.plugins.ble;

import android.Manifest;
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
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.common.collect.ImmutableList;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.joda.time.Duration;

import io.v.v23.context.VContext;
import io.v.v23.discovery.AdId;

import io.v.x.ref.lib.discovery.AdInfo;

import io.v.impl.google.lib.discovery.UUIDUtil;
import io.v.impl.google.lib.discovery.Plugin;

/**
 * The discovery plugin interface for BLE.
 */
public class BlePlugin implements Plugin {
    private static final String TAG = "BlePlugin";

    // We are using a constant for the MTU because Android and paypal/gatt don't get along
    // when the paypal gatt client sends a setMTU message.  The Android server seems to send
    // a malformed L2CAP message.
    private static final int MTU = 23;

    // Default device cache expiration timeout.
    private static final Duration defaultCacheDuration = Duration.standardSeconds(90);

    // Random generator for stamp.
    private final SecureRandom random = new SecureRandom();

    private final Context androidContext;

    // Set of Ble objects that will be interacted with to perform operations.
    private BluetoothLeAdvertiser bluetoothLeAdvertise;
    private BluetoothLeScanner bluetoothLeScanner;
    private BluetoothGattServer bluetoothGattServer;

    private Map<AdId, BluetoothGattService> advertisements;
    private AdvertiseCallback advertiseCallback;

    private Set<Plugin.ScanHandler> scanners;
    private Set<String> pendingConnections;
    private DeviceCache deviceCache;
    private ScanCallback scanCallback;

    // If isEnabled is false, then all operations on the ble plugin are no-oped. This will only
    // be false if the ble hardware is inaccessible.
    private boolean isEnabled = false;

    private boolean hasPermission(String perm) {
        return ContextCompat.checkSelfPermission(androidContext, perm)
                == PackageManager.PERMISSION_GRANTED;
    }

    public BlePlugin(Context androidContext, String host) {
        this.androidContext = androidContext;
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            return;
        }
        if (!hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                && !hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            return;
        }

        bluetoothLeAdvertise = bluetoothAdapter.getBluetoothLeAdvertiser();
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        BluetoothManager manager =
                (BluetoothManager) androidContext.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothGattServer =
                manager.openGattServer(
                        androidContext,
                        new BluetoothGattServerCallback() {
                            @Override
                            public void onConnectionStateChange(
                                    BluetoothDevice device, int status, int newState) {
                                super.onConnectionStateChange(device, status, newState);
                            }

                            @Override
                            public void onCharacteristicReadRequest(
                                    BluetoothDevice device,
                                    int requestId,
                                    int offset,
                                    BluetoothGattCharacteristic characteristic) {
                                super.onCharacteristicReadRequest(
                                        device, requestId, offset, characteristic);
                                byte[] total = characteristic.getValue();
                                byte[] res = {};
                                // Only send MTU - 1 bytes. The first byte of all packets is the op code.
                                if (offset < total.length) {
                                    int finalByte = offset + MTU - 1;
                                    if (finalByte > total.length) {
                                        finalByte = total.length;
                                    }
                                    res = Arrays.copyOfRange(total, offset, finalByte);
                                    bluetoothGattServer.sendResponse(
                                            device, requestId, BluetoothGatt.GATT_SUCCESS, 0, res);
                                } else {
                                    // This should probably be an error, but a bug in the paypal/gatt code causes an
                                    // infinite loop if this returns an error rather than the empty value.
                                    bluetoothGattServer.sendResponse(
                                            device, requestId, BluetoothGatt.GATT_SUCCESS, 0, res);
                                }
                            }
                        });

        advertisements = new HashMap<>();
        scanners = new HashSet<>();
        pendingConnections = new HashSet<>();
        deviceCache = new DeviceCache(defaultCacheDuration);
        isEnabled = true;
    }

    public void startAdvertising(AdInfo adInfo) throws Exception {
        if (!isEnabled) {
            throw new IllegalStateException("BlePlugin not enabled");
        }

        BluetoothGattService service =
                new BluetoothGattService(
                        UUIDUtil.serviceUUID(adInfo.getAd().getInterfaceName()),
                        BluetoothGattService.SERVICE_TYPE_PRIMARY);
        for (Map.Entry<UUID, byte[]> entry : ConvertUtil.toGattAttrs(adInfo).entrySet()) {
            BluetoothGattCharacteristic c =
                    new BluetoothGattCharacteristic(
                            entry.getKey(),
                            BluetoothGattCharacteristic.PROPERTY_READ,
                            BluetoothGattCharacteristic.PERMISSION_READ);
            c.setValue(entry.getValue());
            service.addCharacteristic(c);
        }

        synchronized (advertisements) {
            advertisements.put(adInfo.getAd().getId(), service);
            bluetoothGattServer.addService(service);
            updateAdvertising();
        }
    }

    public void stopAdvertising(AdInfo adInfo) {
        synchronized (advertisements) {
            BluetoothGattService service = advertisements.remove(adInfo.getAd().getId());
            if (service != null) {
                bluetoothGattServer.removeService(service);
                updateAdvertising();
            }
        }
    }

    private long genStamp() {
        // We use 8-byte stamp to reflect the current services of the current device.
        //
        // TODO(bjornick): 8-byte random number might not be good enough for
        // global uniqueness. We might want to consider a better way to generate
        // stamp like using a unique device id with sequence number.
        return new BigInteger(64, random).longValue();
    }

    private void updateAdvertising() {
        if (advertiseCallback != null) {
            bluetoothLeAdvertise.stopAdvertising(advertiseCallback);
            advertiseCallback = null;
        }
        if (advertisements.size() == 0) {
            return;
        }

        AdvertiseData.Builder builder = new AdvertiseData.Builder();
        ByteBuffer buf = ByteBuffer.allocate(9);
        buf.put((byte) 8);
        buf.putLong(genStamp());
        builder.addManufacturerData(1001, buf.array());
        AdvertiseSettings.Builder settingsBuilder = new AdvertiseSettings.Builder();
        settingsBuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY);
        settingsBuilder.setConnectable(true);
        advertiseCallback =
                new AdvertiseCallback() {
                    @Override
                    public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                        Log.d(TAG, "started " + settingsInEffect);
                    }

                    @Override
                    public void onStartFailure(int errorCode) {
                        Log.e(TAG, "failed to start advertising " + errorCode);
                    }
                };
        bluetoothLeAdvertise.startAdvertising(
                settingsBuilder.build(), builder.build(), advertiseCallback);
    }

    public void startScan(String interfaceName, Plugin.ScanHandler handler) throws Exception {
        if (!isEnabled) {
            throw new IllegalStateException("BlePlugin not enabled");
        }

        synchronized (scanners) {
            if (!scanners.add(handler)) {
                throw new IllegalArgumentException("handler already registered");
            }
            deviceCache.addScanner(interfaceName, handler);
            updateScan();
        }
    }

    public void stopScan(Plugin.ScanHandler handler) {
        synchronized (scanners) {
            if (!scanners.remove(handler)) {
                return;
            }
            deviceCache.removeScanner(handler);
            updateScan();
        }
    }

    private void updateScan() {
        // TODO(jhahn): Verify whether we need to stop scanning while connect to remote GATT servers.
        if (scanners.isEmpty()) {
            if (pendingConnections.isEmpty()) {
                bluetoothLeScanner.stopScan(scanCallback);
                scanCallback = null;
            }
            return;
        }
        if (scanCallback != null) {
            return;
        }

        final List<ScanFilter> scanFilters =
                ImmutableList.of(
                        new ScanFilter.Builder()
                                .setManufacturerData(1001, new byte[0], new byte[0])
                                .build());
        final ScanSettings scanSettings =
                new ScanSettings.Builder()
                        .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                        .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
                        .build();
        scanCallback =
                new ScanCallback() {
                    @Override
                    public void onScanResult(int callbackType, ScanResult result) {
                        ScanRecord record = result.getScanRecord();
                        // Use 1001 to denote that this is a Vanadium device.  We picked an id that is
                        // currently not in use.
                        byte[] data = record.getManufacturerSpecificData(1001);
                        ByteBuffer buffer = ByteBuffer.wrap(data);
                        final long stamp = buffer.getLong();
                        final String deviceId = result.getDevice().getAddress();
                        if (deviceCache.haveSeenStamp(stamp, deviceId)) {
                            return;
                        }

                        BluetoothGattReader.Handler handler =
                                new BluetoothGattReader.Handler() {
                                    @Override
                                    public void handle(Map<UUID, Map<UUID, byte[]>> services) {
                                        if (services != null) {
                                            List<AdInfo> adInfos = new ArrayList<>();
                                            for (Map.Entry<UUID, Map<UUID, byte[]>> entry :
                                                    services.entrySet()) {
                                                try {
                                                    AdInfo adInfo =
                                                            ConvertUtil.toAdInfo(entry.getValue());
                                                    adInfos.add(adInfo);
                                                } catch (IOException e) {
                                                    Log.e(
                                                            TAG,
                                                            "failed to convert advertisement" + e);
                                                }
                                            }
                                            deviceCache.saveDevice(stamp, deviceId, adInfos);
                                        }
                                        synchronized (scanners) {
                                            pendingConnections.remove(deviceId);
                                            if (pendingConnections.isEmpty()) {
                                                if (scanners.isEmpty()) {
                                                    scanCallback = null;
                                                    return;
                                                }
                                                bluetoothLeScanner.startScan(
                                                        scanFilters, scanSettings, scanCallback);
                                            }
                                        }
                                    }
                                };
                        BluetoothGattReader cb = new BluetoothGattReader(handler);
                        synchronized (scanners) {
                            if (scanners.isEmpty()) {
                                return;
                            }
                            if (!pendingConnections.add(deviceId)) {
                                return;
                            }
                            if (pendingConnections.size() == 1) {
                                bluetoothLeScanner.stopScan(scanCallback);
                            }
                        }
                        Log.d(TAG, "connecting to " + result.getDevice());
                        result.getDevice().connectGatt(androidContext, false, cb);
                    }

                    @Override
                    public void onBatchScanResults(List<ScanResult> results) {}

                    @Override
                    public void onScanFailed(int errorCode) {}
                };
        bluetoothLeScanner.startScan(scanFilters, scanSettings, scanCallback);
    }
}
