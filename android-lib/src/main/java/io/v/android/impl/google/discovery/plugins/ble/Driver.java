// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.impl.google.discovery.plugins.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.ParcelUuid;
import android.util.Log;
import android.util.Pair;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.v.android.v23.V;
import io.v.v23.context.VContext;

/**
 * A BLE Driver for Android.
 */
public class Driver implements GattReader.Handler {
    static final String TAG = "BleDriver";

    /**
     * An interface for passing scanned advertisements.
     */
    public interface ScanHandler {
        /**
         * Called with each discovery update.
         */
        void onDiscovered(String uuid, Map<String, byte[]> characteristics, int rssi);
    }

    private Context mContext;

    private final BluetoothAdapter mBluetoothAdapter;

    private BluetoothLeAdvertiser mAdvertiser;
    private Map<UUID, AdvertiseCallback> mAdvertiseCallbacks;
    private GattServer mGattServer;

    private final Map<String, BluetoothGattService> mGattServices;

    private BluetoothLeScanner mScanner;
    private ScanCallback mScanCallback;
    private GattReader mGattReader;

    private String[] mScanUuids;
    private String mScanBaseUuid, mScanMaskUuid;
    private ScanHandler mScanHandler;
    private Map<Pair<BluetoothDevice, UUID>, Integer> mScanSeens;

    private boolean mEnabled;
    private int mOnServiceReadCallbacks;

    private final class BluetoothAdapterStatusReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!intent.getAction().equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                return;
            }
            switch (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                case BluetoothAdapter.STATE_ON:
                    resume();
                    break;
                case BluetoothAdapter.STATE_OFF:
                    pause();
                    break;
            }
        }
    }

    /**
     * Create a new BLE driver for Android.
     *
     * @param vContext Vanadium context.
     */
    public Driver(VContext vContext) {
        mContext = V.getAndroidContext(vContext);
        if (mContext == null) {
            throw new IllegalStateException("AndroidContext not available");
        }
        mGattServices = new HashMap<>();

        BluetoothManager manager =
                ((BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE));
        mBluetoothAdapter = manager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.w(TAG, "BluetoothAdapter not available");
            return;
        }

        mContext.registerReceiver(
                new BluetoothAdapterStatusReceiver(),
                new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        if (!mBluetoothAdapter.isEnabled()) {
            // Prompt user to turn on Bluetooth.
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            enableBtIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivity(enableBtIntent);
            return;
        }
        resume();
    }

    private synchronized void resume() {
        if (mEnabled) {
            return;
        }
        if (!mBluetoothAdapter.isEnabled()) {
            return;
        }
        mEnabled = true;

        resumeAdvertisingSynchronized();
        resumeScanningSynchronized();

        Log.i(TAG, "started");
    }

    private synchronized void pause() {
        if (!mEnabled) {
            return;
        }
        mEnabled = false;

        pauseAdvertisingSynchronized();
        pauseScanningSynchronized();

        Log.i(TAG, "stopped");
    }

    public void addService(final String uuid, Map<String, byte[]> characteristics) {
        BluetoothGattService service =
                new BluetoothGattService(
                        UUID.fromString(uuid), BluetoothGattService.SERVICE_TYPE_PRIMARY);
        for (Map.Entry<String, byte[]> entry : characteristics.entrySet()) {
            BluetoothGattCharacteristic characteristic =
                    new BluetoothGattCharacteristic(
                            UUID.fromString(entry.getKey()),
                            BluetoothGattCharacteristic.PROPERTY_READ,
                            BluetoothGattCharacteristic.PERMISSION_READ);
            characteristic.setValue(entry.getValue());
            service.addCharacteristic(characteristic);
        }

        synchronized (this) {
            if (mGattServices.put(uuid, service) != null) {
                throw new IllegalStateException("already being advertised: " + uuid);
            }
            if (mEnabled && mBluetoothAdapter.isMultipleAdvertisementSupported()) {
                startAdvertisingSynchronized(service);
            }
        }
    }

    public synchronized void removeService(String uuid) {
        BluetoothGattService service = mGattServices.remove(uuid);
        if (service == null) {
            return;
        }
        if (mEnabled && mBluetoothAdapter.isMultipleAdvertisementSupported()) {
            stopAdvertisingSynchronized(service);
        }
    }

    private void startAdvertisingSynchronized(BluetoothGattService service) {
        mGattServer.addService(service);

        final UUID uuid = service.getUuid();
        AdvertiseSettings settings =
                new AdvertiseSettings.Builder()
                        .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                        .setConnectable(true)
                        .build();
        AdvertiseData data =
                new AdvertiseData.Builder()
                        .addServiceUuid(new ParcelUuid(uuid))
                        .setIncludeTxPowerLevel(true)
                        .build();
        AdvertiseCallback callback =
                new AdvertiseCallback() {
                    @Override
                    public void onStartFailure(int errorCode) {
                        Log.e(TAG, "startAdvertising failed: " + uuid + ", errorCode:" + errorCode);
                    }
                };
        // TODO(jhahn): The maximum number of simultaneous advertisements is limited by the chipset.
        // Rotate active advertisements periodically if the total number of advertisement exceeds
        // the limit.
        mAdvertiser.startAdvertising(settings, data, callback);
        mAdvertiseCallbacks.put(uuid, callback);
    }

    private void stopAdvertisingSynchronized(BluetoothGattService service) {
        mGattServer.removeService(service);

        AdvertiseCallback callback = mAdvertiseCallbacks.remove(service.getUuid());
        mAdvertiser.stopAdvertising(callback);
    }

    private void resumeAdvertisingSynchronized() {
        if (!mBluetoothAdapter.isMultipleAdvertisementSupported()) {
            Log.w(TAG, "advertisement is not supported by this device");
            return;
        }

        mAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
        mAdvertiseCallbacks = new HashMap<>();
        mGattServer = new GattServer(mContext);
        for (BluetoothGattService service : mGattServices.values()) {
            startAdvertisingSynchronized(service);
        }
    }

    private void pauseAdvertisingSynchronized() {
        if (mGattServer != null) {
            mGattServer.close();
            mGattServer = null;
        }

        // mAdvertiser is invalidated when BluetoothAdapter is turned off.
        // We don't need to stop any active advertising.
        mAdvertiser = null;
        mAdvertiseCallbacks = null;
    }

    public synchronized void startScan(
            String[] uuids, String baseUuid, String maskUuid, ScanHandler handler) {
        if (mScanHandler != null) {
            throw new IllegalStateException("scan already started");
        }

        mScanUuids = uuids;
        mScanBaseUuid = baseUuid;
        mScanMaskUuid = maskUuid;
        mScanHandler = handler;
        if (mEnabled) {
            startScanningSynchronized();
        }
    }

    public synchronized void stopScan() {
        if (mScanHandler == null) {
            return;
        }

        mScanUuids = null;
        mScanBaseUuid = null;
        mScanMaskUuid = null;
        mScanHandler = null;
        if (mEnabled) {
            stopScanningSynchronized();
        }
    }

    private void startScanningSynchronized() {
        mGattReader = new GattReader(mContext, this);
        mScanSeens = new HashMap();

        List<ScanFilter> filters = null;
        if (mScanUuids != null) {
            ImmutableList.Builder<ScanFilter> builder = new ImmutableList.Builder();
            for (String uuid : mScanUuids) {
                builder.add(
                        new ScanFilter.Builder()
                                .setServiceUuid(ParcelUuid.fromString(uuid))
                                .build());
            }
            filters = builder.build();
        }
        ScanSettings settings =
                new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_BALANCED).build();

        // ScanFilter doesn't work with startScan() if there are too many - more than 63bits - ignore
        // bits. So we call startScan() without a scan filter for base/mask uuids and match scan results
        // against it.
        final ScanFilter matcher =
                new ScanFilter.Builder()
                        .setServiceUuid(
                                ParcelUuid.fromString(mScanBaseUuid),
                                ParcelUuid.fromString(mScanMaskUuid))
                        .build();

        mScanCallback =
                new ScanCallback() {
                    @Override
                    public void onScanResult(int callbackType, ScanResult result) {
                        if (callbackType == ScanSettings.CALLBACK_TYPE_MATCH_LOST) {
                            // This callback will never be called with this callback type, since the
                            // scan setting is for CALLBACK_TYPE_ALL_MATCHES. But just for safety.
                            return;
                        }
                        if (!matcher.matches(result)) {
                            return;
                        }
                        ScanRecord scanRecord = result.getScanRecord();
                        if (scanRecord.getServiceUuids().size() != 1) {
                            // This shouldn't happen since we advertise only one uuid in each advertisement.
                            return;
                        }
                        UUID uuid = scanRecord.getServiceUuids().get(0).getUuid();
                        Pair<BluetoothDevice, UUID> seen = Pair.create(result.getDevice(), uuid);
                        synchronized (Driver.this) {
                            if (mEnabled && mScanSeens.put(seen, result.getRssi()) == null) {
                                mGattReader.readService(result.getDevice(), uuid);
                            }
                        }
                    }

                    @Override
                    public void onScanFailed(int errorCode) {
                        Log.e(TAG, "startScan failed: " + errorCode);
                    }
                };
        mScanner.startScan(filters, settings, mScanCallback);
    }

    private void stopScanningSynchronized() {
        mScanner.stopScan(mScanCallback);
        mScanCallback = null;
        mScanSeens = null;

        mGattReader.close();
        mGattReader = null;
    }

    private void resumeScanningSynchronized() {
        mScanner = mBluetoothAdapter.getBluetoothLeScanner();
        if (mScanHandler != null) {
            startScanningSynchronized();
        }
    }

    private void pauseScanningSynchronized() {
        if (mScanHandler != null) {
            mGattReader.close();
            mGattReader = null;

            // mScanner is invalidated when BluetoothAdapter is turned off.
            // We don't need to stop any active scan.
            mScanner = null;
            mScanCallback = null;
            mScanSeens = null;
        }
    }

    public void onServiceRead(BluetoothDevice device, BluetoothGattService service) {
        Map<String, byte[]> characteristics;
        ImmutableMap.Builder<String, byte[]> builder = new ImmutableMap.Builder();
        for (BluetoothGattCharacteristic c : service.getCharacteristics()) {
            builder.put(c.getUuid().toString(), c.getValue());
        }
        characteristics = builder.build();

        synchronized (this) {
            mOnServiceReadCallbacks++;
            if (mScanHandler == null) {
                return;
            }
            Integer rssi = mScanSeens.get(Pair.create(device, service.getUuid()));
            if (rssi == null) {
                return;
            }
            mScanHandler.onDiscovered(
                    service.getUuid().toString(), characteristics, rssi);
        }
    }

    public synchronized void onServiceReadFailed(BluetoothDevice device, UUID uuid) {
        // Remove the seen record to retry to read the service.
        mScanSeens.remove(Pair.create(device, uuid));
    }

    public synchronized String debugString() {
        if (mBluetoothAdapter == null) {
            return "Not available";
        }
        StringBuilder b = new StringBuilder().append("BluetoothAdapter: ");
        switch (mBluetoothAdapter.getState()) {
            case BluetoothAdapter.STATE_ON:
                b.append("ON");
                break;
            case BluetoothAdapter.STATE_TURNING_ON:
                b.append("Turning on");
                break;
            case BluetoothAdapter.STATE_OFF:
                b.append("OFF");
                break;
            case BluetoothAdapter.STATE_TURNING_OFF:
                b.append("Turning off");
                break;
            default:
                b.append("Unknown state");
                break;
        }
        b.append("\n");
        b.append("ENABLED: ").append(mEnabled).append("\n");
        if (mGattServices.size() > 0) {
            b.append("ADVERTISING ").append(mGattServices.size()).append(" services\n");
        }
        if (mScanCallback != null) {
            b.append("SCANNING\n");
        }
        b.append("OnServiceReadCallbacks: ").append(mOnServiceReadCallbacks).append("\n");
        return b.toString();
    }
}
