// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.impl.google.discovery.plugins.ble;

import android.Manifest;
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
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.ParcelUuid;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import io.v.android.v23.V;
import io.v.v23.context.VContext;

/**
 * A BLE Driver for Android.
 *
 * This Driver also support discovery over Bluetooth classic by
 *    - Each peripheral makes the device discoverable for a specified duration.
 *    - A central device discovers near-by devices through Bluetooth classic,
 *      tries to connect and check each device whether the device has any
 *      services that the central is looking for. A central device will fetch
 *      services through Gatt over BR/EDR.
 */
public class Driver implements BluetoothScanner.Handler, GattReader.Handler {
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

    private final Context mContext;

    private final BluetoothAdapter mBluetoothAdapter;

    private BluetoothAdvertiser mClassicAdvertiser;
    private static int sClassicDiscoverableDurationInSec;
    private BluetoothLeAdvertiser mLeAdvertiser;
    private Map<UUID, AdvertiseCallback> mLeAdvertiseCallbacks;
    private GattServer mGattServer;

    private final Map<String, BluetoothGattService> mServices;

    private BluetoothScanner mClassicScanner;
    private static boolean sClassicScanEnabled;
    private BluetoothLeScanner mLeScanner;
    private ScanCallback mLeScanCallback;
    private GattReader mGattReader;

    private Set<UUID> mScanUuids;
    private ParcelUuid mScanBaseUuid, mScanMaskUuid;
    private ScanHandler mScanHandler;
    private Map<BluetoothDevice, Integer> mScanSeens;

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
        mServices = new HashMap<>();

        BluetoothManager manager =
                ((BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE));
        mBluetoothAdapter = manager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.w(TAG, "BluetoothAdapter not available");
            return;
        }

        if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(
                                mContext, Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            Log.w(
                    TAG,
                    "ACCESS_FINE_LOCATION or ACCESS_COARSE_LOCATION not granted, "
                            + "Bluetooth discovery will not be happening");
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

        resumeAdvertising();
        resumeScanning();

        Log.i(TAG, "started");
    }

    private synchronized void pause() {
        if (!mEnabled) {
            return;
        }
        mEnabled = false;

        pauseAdvertising();
        pauseScanning();

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
            if (mServices.put(uuid, service) != null) {
                throw new IllegalStateException("already being advertised: " + uuid);
            }
            if (mEnabled) {
                startAdvertising(service);
            }
        }
    }

    public synchronized void removeService(String uuid) {
        BluetoothGattService service = mServices.remove(uuid);
        if (service == null) {
            return;
        }
        if (mEnabled) {
            stopAdvertising(service);
        }
    }

    private synchronized void startAdvertising(BluetoothGattService service) {
        mGattServer.addService(service);
        synchronized (Driver.class) {
            mClassicAdvertiser.addService(service.getUuid(), sClassicDiscoverableDurationInSec);
            sClassicDiscoverableDurationInSec = 0;
        }
        if (mLeAdvertiser != null) {
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
                            Log.e(
                                    TAG,
                                    "startAdvertising failed: "
                                            + uuid
                                            + ", errorCode:"
                                            + errorCode);
                        }
                    };
            // TODO(jhahn): The maximum number of simultaneous advertisements is limited by the chipset.
            // Rotate active advertisements periodically if the total number of advertisement exceeds
            // the limit.
            mLeAdvertiser.startAdvertising(settings, data, callback);
            mLeAdvertiseCallbacks.put(uuid, callback);
        }
    }

    private synchronized void stopAdvertising(BluetoothGattService service) {
        mGattServer.removeService(service);
        mClassicAdvertiser.removeService(service.getUuid());
        if (mLeAdvertiser != null) {
            AdvertiseCallback callback = mLeAdvertiseCallbacks.remove(service.getUuid());
            mLeAdvertiser.stopAdvertising(callback);
        }
    }

    private synchronized void resumeAdvertising() {
        mGattServer = new GattServer(mContext);
        mClassicAdvertiser = new BluetoothAdvertiser(mContext, mBluetoothAdapter);
        if (mBluetoothAdapter.isMultipleAdvertisementSupported()) {
            mLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
            mLeAdvertiseCallbacks = new HashMap<>();
        }

        for (BluetoothGattService service : mServices.values()) {
            startAdvertising(service);
        }
    }

    private synchronized void pauseAdvertising() {
        mGattServer.close();
        mGattServer = null;
        mClassicAdvertiser.close();
        mClassicAdvertiser = null;

        // mLeAdvertiser is invalidated when BluetoothAdapter is turned off.
        // We don't need to stop any active advertising.
        mLeAdvertiser = null;
        mLeAdvertiseCallbacks = null;
    }

    public synchronized void startScan(
            String[] uuids, String baseUuid, String maskUuid, ScanHandler handler) {
        if (mScanHandler != null) {
            throw new IllegalStateException("scan already started");
        }

        ImmutableSet.Builder<UUID> builder = ImmutableSet.builder();
        if (uuids != null) {
            for (String uuid : uuids) {
                builder.add(UUID.fromString(uuid));
            }
        }
        mScanUuids = builder.build();
        mScanBaseUuid = ParcelUuid.fromString(baseUuid);
        mScanMaskUuid = ParcelUuid.fromString(maskUuid);
        mScanHandler = handler;
        if (mEnabled) {
            startScanning();
        }
    }

    public synchronized void stopScan() {
        if (mScanHandler == null) {
            return;
        }

        if (mEnabled) {
            stopScanning();
        }
        mScanUuids = null;
        mScanBaseUuid = null;
        mScanMaskUuid = null;
        mScanHandler = null;
    }

    private synchronized void startScanning() {
        mScanSeens = new HashMap<>();
        mGattReader =
                new GattReader(
                        mContext,
                        mScanUuids,
                        mScanBaseUuid.getUuid(),
                        mScanMaskUuid.getUuid(),
                        this);
        synchronized (Driver.class) {
            if (sClassicScanEnabled) {
                // Note that BluetoothLeScan will be started when BluetoothScan finishes.
                mClassicScanner.startScan(mScanUuids);
                sClassicScanEnabled = false;
            } else {
                startBluetoothLeScanner();
            }
        }
    }

    private synchronized void startBluetoothLeScanner() {
        if (mLeScanner == null) {
            return;
        }

        ImmutableList.Builder<ScanFilter> builder = new ImmutableList.Builder();
        for (UUID uuid : mScanUuids) {
            builder.add(new ScanFilter.Builder().setServiceUuid(new ParcelUuid(uuid)).build());
        }
        List<ScanFilter> filters = builder.build();

        ScanSettings settings =
                new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_BALANCED).build();

        // ScanFilter doesn't work with startScan() if there are too many - more than 63bits - ignore
        // bits. So we call startScan() without a scan filter for base/mask uuids and match scan results
        // against it.
        final ScanFilter matcher =
                new ScanFilter.Builder().setServiceUuid(mScanBaseUuid, mScanMaskUuid).build();

        mLeScanCallback =
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
                        BluetoothDevice device = result.getDevice();
                        synchronized (Driver.this) {
                            if (mScanSeens != null
                                    && mScanSeens.put(device, result.getRssi()) == null) {
                                mGattReader.readDevice(device);
                            }
                        }
                    }

                    @Override
                    public void onScanFailed(int errorCode) {
                        Log.e(TAG, "startScan failed: " + errorCode);
                    }
                };

        mLeScanner.startScan(filters, settings, mLeScanCallback);
    }

    private synchronized void stopScanning() {
        mClassicScanner.stopScan();
        if (mLeScanCallback != null) {
            mLeScanner.stopScan(mLeScanCallback);
            mLeScanCallback = null;
        }
        mGattReader.close();
        mGattReader = null;
        mScanSeens = null;
    }

    private synchronized void resumeScanning() {
        mClassicScanner = new BluetoothScanner(mContext, mBluetoothAdapter, this);
        mLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        if (mScanHandler != null) {
            startScanning();
        }
    }

    private synchronized void pauseScanning() {
        mClassicScanner.close();
        mClassicScanner = null;
        if (mScanHandler != null) {
            mGattReader.close();
            mGattReader = null;

            // mLeScanner is invalidated when BluetoothAdapter is turned off.
            // We don't need to stop any active scan.
            mLeScanner = null;
            mLeScanCallback = null;
            mScanSeens = null;
        }
    }

    public synchronized void onBluetoothDiscoveryFinished(Map<BluetoothDevice, Integer> found) {
        if (mScanSeens == null) {
            return;
        }

        // Start to read services through Gatt.
        //
        // TODO(jhahn): Do we need to retry when Gatt read fails?
        for (Map.Entry<BluetoothDevice, Integer> e : found.entrySet()) {

            mScanSeens.put(e.getKey(), e.getValue());
            mGattReader.readDevice(e.getKey());
        }

        // Now start BluetoothLeScan.
        startBluetoothLeScanner();
    }

    public void onGattRead(BluetoothDevice device, BluetoothGattService service) {
        Map<String, byte[]> characteristics;
        ImmutableMap.Builder<String, byte[]> builder = new ImmutableMap.Builder();
        for (BluetoothGattCharacteristic c : service.getCharacteristics()) {
            builder.put(c.getUuid().toString(), c.getValue());
        }
        characteristics = builder.build();

        synchronized (this) {
            if (mScanSeens == null) {
                return;
            }
            Integer rssi = mScanSeens.get(device);
            if (rssi == null) {
                return;
            }
            mScanHandler.onDiscovered(service.getUuid().toString(), characteristics, rssi);
            mOnServiceReadCallbacks++;
        }
    }

    public synchronized void onGattReadFailed(BluetoothDevice device) {
        if (mScanSeens == null) {
            return;
        }

        // Remove the seen record to retry to read the device.
        mScanSeens.remove(device);
    }

    /**
     * Set the Duration of Bluetooth discoverability in seconds. This will be applied for
     * the next addService() only one time.
     *
     * TODO(jhahn): Find a better API to set Bluetooth discovery options.
     */
    public static synchronized void setBluetoothDiscoverableDuration(int durationInSec) {
        sClassicDiscoverableDurationInSec = durationInSec;
    }

    /**
     * Enable Bluetooth scan. This will be applied for the next startScan() only one time.
     *
     * TODO(jhahn): Find a better API to set Bluetooth discovery options.
     */
    public static synchronized void setBluetoothScanEnabled(boolean enabled) {
        sClassicScanEnabled = enabled;
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
        if (mServices.size() > 0) {
            b.append("ADVERTISING ").append(mServices.size()).append(" services\n");
        }
        if (mLeScanCallback != null) {
            b.append("SCANNING\n");
        }
        b.append("OnServiceReadCallbacks: ").append(mOnServiceReadCallbacks).append("\n");
        return b.toString();
    }
}
