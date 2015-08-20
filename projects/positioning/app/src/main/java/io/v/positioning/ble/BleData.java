// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.positioning.ble;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * BleData class is used to encapsulate a single BLE package that will be sent or received.
 * Note that we are limited to 20 (currently using 16) bytes of data. This byte array is passed to
 * the AdvertiseData {@link  android.bluetooth.le.AdvertiseData.Builder}  for advertisement.
 */
public class BleData {
    int deviceId = 0;
    int roundNumber = 0;
    long time = 0;

    public BleData(int deviceId, int roundNumber, long time) {
        this.deviceId = deviceId;
        this.roundNumber = roundNumber;
        this.time = time;
    }

    BleData(byte[] receivedBytes) {
        unpackageData(receivedBytes);
    }

    protected byte[] packageData() {
        ByteBuffer bb = ByteBuffer.allocate(16);
        bb.order(ByteOrder.nativeOrder());
        bb.putInt(0, deviceId);
        bb.putInt(4, roundNumber);
        bb.putLong(8, time);
        return bb.array();
    }

    private void unpackageData(byte[] array) {
        ByteBuffer bb = ByteBuffer.wrap(array);
        bb.order(ByteOrder.nativeOrder());
        deviceId = bb.getInt(0);
        roundNumber = bb.getInt(4);
        time = bb.getLong(8);
    }

    public void setBleData(int deviceId, int roundNumber, long time) {
        this.deviceId = deviceId;
        this.roundNumber = roundNumber;
        this.time = time;
    }

    @Override
    public String toString() {
        return "deviceId: " + deviceId + " roundNumber " + roundNumber + " time " + time;
    }
}