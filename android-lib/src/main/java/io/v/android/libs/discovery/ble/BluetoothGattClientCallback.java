// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.libs.discovery.ble;

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
public class BluetoothGattClientCallback extends BluetoothGattCallback {
    /**
     * A handler that will get called when all the services from a gatt service
     * are read.
     */
    public interface Callback {
        /**
         * Called with the map of service ids to their attributes.
         * @param services A map from service id to (characteristics uuid to values).
         */
        void handle(Map<UUID, Map<UUID, byte[]>> services);
    }
    // We want to ignore the GATT and GAP services, which are 1800 and 1801 respectively.
    static final String GATT_AND_GAP_PREFIX = "0000180";

    private final Callback callback;

    private final Map<UUID, Map<UUID, byte[]>> services = new HashMap<>();

    private BluetoothGatt gatt;

    private final List<BluetoothGattCharacteristic> chars = new ArrayList<>();
    private int pos;

    BluetoothGattClientCallback(Callback cb) {
        callback = cb;
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        for (BluetoothGattService service : gatt.getServices()) {
            Log.d("vanadium", "Saw service" + service.getUuid().toString());
            // Skip the GATT AND GAP Services.
            if (service.getUuid().toString().startsWith(GATT_AND_GAP_PREFIX)) {
                continue;
            }
            services.put(service.getUuid(), new HashMap<UUID, byte[]>());
            // We only keep track of the characteristics that can be read.
            for (BluetoothGattCharacteristic ch : service.getCharacteristics()) {
                if ((ch.getProperties() & BluetoothGattCharacteristic.PROPERTY_READ) != 0) {
                    chars.add(ch);
                } else {
                    Log.d("vanadium", "skipping non read property");
                }
            }
        }
        pos = 0;
        maybeReadNextCharacteristic();
    }

    // Reads the next characteristic if there is one.  Otherwise calls callback and
    // closes the gatt connection.
    private void maybeReadNextCharacteristic() {
        if (pos >= chars.size()) {
            gatt.disconnect();
            gatt.close();
            callback.handle(services);
            return;
        }
        BluetoothGattCharacteristic c = chars.get(pos++);
        if (!gatt.readCharacteristic(c)) {
            Log.d("vanadium", "Failed to read characteristic " + c.getUuid());
            maybeReadNextCharacteristic();
        }
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic,
                                     int status) {
        UUID serviceUUID = characteristic.getService().getUuid();
        Log.d("vanadium", "Got characteristic [" + serviceUUID + "]"
                + characteristic.getUuid() + "=" + characteristic.getValue());
        services.get(serviceUUID).put(characteristic.getUuid(), characteristic.getValue());
        maybeReadNextCharacteristic();
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        Log.d("vanadium", "new connections state is " + newState);

        this.gatt = gatt;
        if (status != BluetoothGatt.GATT_SUCCESS || newState != BluetoothGatt.STATE_CONNECTED) {
            Log.d("vanadium", "failed to connect with status " + status + " state" + newState);
            gatt.close();
            callback.handle(null);
            return;
        }
        gatt.discoverServices();
    }
}
