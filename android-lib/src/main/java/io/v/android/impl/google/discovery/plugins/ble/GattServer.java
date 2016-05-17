// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.impl.google.discovery.plugins.ble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * A server that serves Vanadium Gatt services to remote Gatt readers.
 */
class GattServer extends BluetoothGattServerCallback {
    // L2CAP implementations shall support a minimum MTU size of 23 octets.
    // See Bluetooth specification version 4.2 section 5.1.
    private static final int DEFAULT_MTU = 23;

    private final BluetoothGattServer mBluetoothGattServer;
    private final Map<BluetoothDevice, Integer> mMtus;

    GattServer(Context context) {
        BluetoothManager manager =
                ((BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE));
        mBluetoothGattServer = manager.openGattServer(context, this);
        mMtus = new HashMap<BluetoothDevice, Integer>();
    }

    /**
     * Add a service to the Gatt server.
     */
    boolean addService(BluetoothGattService service) {
        return mBluetoothGattServer.addService(service);
    }

    /**
     * Removes a service from the Gatt server.
     */
    boolean removeService(BluetoothGattService service) {
        return mBluetoothGattServer.removeService(service);
    }

    /**
     * Closes the Gatt server removing all services.
     */
    void close() {
        mBluetoothGattServer.clearServices();
        mBluetoothGattServer.close();
    }

    @Override
    public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
        super.onConnectionStateChange(device, status, newState);

        if (status != BluetoothGatt.GATT_SUCCESS || newState != BluetoothProfile.STATE_CONNECTED) {
            mMtus.remove(device);
        }
    }

    @Override
    public void onMtuChanged(BluetoothDevice device, int mtu) {
        super.onMtuChanged(device, mtu);

        mMtus.put(device, new Integer(mtu));
    }

    @Override
    public void onCharacteristicReadRequest(
            BluetoothDevice device,
            int requestId,
            int offset,
            BluetoothGattCharacteristic characteristic) {
        super.onCharacteristicReadRequest(device, requestId, offset, characteristic);

        byte[] data = characteristic.getValue();
        if (offset >= data.length) {
            data = null;
        } else {
            int mtu = DEFAULT_MTU;
            if (mMtus.containsKey(device)) {
                mtu = mMtus.get(device).intValue();
            }
            // We can send data up to MTU - 1 bytes.
            int to = offset + mtu - 1;
            if (to > data.length) {
                to = data.length;
            }
            data = Arrays.copyOfRange(data, offset, to);
        }
        mBluetoothGattServer.sendResponse(
                device, requestId, BluetoothGatt.GATT_SUCCESS, offset, data);
    }
}
