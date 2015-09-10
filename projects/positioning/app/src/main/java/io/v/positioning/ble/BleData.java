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
    private final int deviceId;
    private final int roundNumber;
    private final long time;

    public BleData(int deviceId, int roundNumber, long time) {
        this.deviceId = deviceId;
        this.roundNumber = roundNumber;
        this.time = time;
    }

    public BleData(byte[] receivedBytes) {
        ByteBuffer bb = ByteBuffer.wrap(receivedBytes);
        bb.order(ByteOrder.nativeOrder());
        deviceId = bb.getInt(0);
        roundNumber = bb.getInt(4);
        time = bb.getLong(8);
    }

    protected byte[] packageData() {
        ByteBuffer bb = ByteBuffer.allocate(16);
        bb.order(ByteOrder.nativeOrder());
        bb.putInt(0, deviceId);
        bb.putInt(4, roundNumber);
        bb.putLong(8, time);
        return bb.array();
    }

    @Override
    public String toString() {
        return "deviceId: " + deviceId + " roundNumber " + roundNumber + " time " + time;
    }

    public int getDeviceId() {
        return deviceId;
    }

    public int getRoundNumber() {
        return roundNumber;
    }

    public long getTime() {
        return time;
    }

}