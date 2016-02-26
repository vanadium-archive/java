// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.impl.google.discovery.plugins.ble;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A handler for responses from a GattServer.
 */
class BluetoothGattReader extends BluetoothGattCallback {
    private static final String TAG = "BluetoothGattClientCallback";

    // A handler that will get called when all the services from a GATT service are read.
    interface Handler {
        /**
         * Called with the map of service ids to their attributes.
         *
         * @param services A map from service id to (characteristics uuid to values).
         */
        void handle(Map<UUID, Map<UUID, byte[]>> services);
    }

    // We want to ignore the GATT and GAP services, which are 1800 and 1801 respectively.
    static final String GATT_AND_GAP_PREFIX = "0000180";

    private final Handler handler;
    private final Map<UUID, Map<UUID, byte[]>> services = new HashMap<>();

    private BluetoothGatt gatt;

    private final List<BluetoothGattCharacteristic> characteristics = new ArrayList<>();
    private int characteristicsIndex;

    BluetoothGattReader(Handler handler) {
        this.handler = handler;
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        for (BluetoothGattService service : gatt.getServices()) {
            Log.d(TAG, "found service" + service.getUuid().toString());
            // Skip the GATT AND GAP Services.
            if (service.getUuid().toString().startsWith(GATT_AND_GAP_PREFIX)) {
                continue;
            }

            services.put(service.getUuid(), new HashMap<UUID, byte[]>());
            // We only keep track of the characteristics that can be read.
            for (BluetoothGattCharacteristic c : service.getCharacteristics()) {
                if ((c.getProperties() & BluetoothGattCharacteristic.PROPERTY_READ) != 0) {
                    characteristics.add(c);
                } else {
                    Log.d(TAG, "skipping non read property");
                }
            }
        }
        characteristicsIndex = 0;
        maybeReadNextCharacteristic();
    }

    // Reads the next characteristic if there is one. Otherwise calls handler and
    // closes the GATT connection.
    private void maybeReadNextCharacteristic() {
        if (characteristicsIndex >= characteristics.size()) {
            gatt.disconnect();
            gatt.close();
            handler.handle(services);
            return;
        }
        BluetoothGattCharacteristic c = characteristics.get(characteristicsIndex++);
        if (!gatt.readCharacteristic(c)) {
            Log.w(TAG, "failed to read characteristic " + c.getUuid());
            maybeReadNextCharacteristic();
        }
    }

    @Override
    public void onCharacteristicRead(
            BluetoothGatt gatt, BluetoothGattCharacteristic c, int status) {
        UUID serviceUuid = c.getService().getUuid();
        Log.d(TAG, "got characteristic [" + serviceUuid + "]" + c.getUuid() + "=" + c.getValue());

        services.get(serviceUuid).put(c.getUuid(), c.getValue());
        maybeReadNextCharacteristic();
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        Log.d(TAG, "new connections state is " + newState);

        this.gatt = gatt;
        if (status != BluetoothGatt.GATT_SUCCESS || newState != BluetoothGatt.STATE_CONNECTED) {
            Log.w(TAG, "failed to connect with status " + status + " state" + newState);
            gatt.close();
            handler.handle(null);
            return;
        }
        gatt.discoverServices();
    }
}
