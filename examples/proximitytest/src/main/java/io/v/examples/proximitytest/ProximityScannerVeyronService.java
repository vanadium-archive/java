// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.


package io.v.examples.proximitytest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothManager;
import android.util.Log;

import io.v.v23.context.VContext;
import io.v.v23.rpc.ServerCall;
import io.v.v23.verror.VException;

public class ProximityScannerVeyronService implements ProximityScannerServer {

    private final BluetoothScanner scanner;

    public ProximityScannerVeyronService(BluetoothScanner scanner) {
        this.scanner = scanner;
    }

    public static ProximityScannerVeyronService create(final BluetoothManager bluetoothManager)
            throws BluetoothNotEnabledException {
        final BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            throw new BluetoothNotEnabledException(
                    "Creating service failed because bluetooth not enabled");
        }

        final BluetoothScanner scanner = new BluetoothScanner(bluetoothAdapter);
        return new ProximityScannerVeyronService(scanner);
    }

    private int computeAverageDBm(List<BluetoothScanner.Reading> readings) {
        if (readings.size() <= 0) {
            return Integer.MIN_VALUE;
        }
        // Trim out smallest and highest 33% of readings.
        final int trimSize = readings.size() / 3;
        final List<BluetoothScanner.Reading> trimmedReadings = readings.subList(trimSize,
                readings.size() - trimSize);
        if (trimmedReadings.isEmpty()) {
            // This should never happen as:
            // - readings.size() > 0 and,
            // - we trim floor(2 * readings.size() / 3) elements from
            // 'readings', which leaves at least ceil(readings.size() / 3)
            // elements, which is greater than 0 when readings.size() > 0.
            Log.e("ProximityScannerVeyronService", "Trimmed readings list is unexpectedly empty.");
            return Integer.MIN_VALUE;
        }
        int totaldBm = 0;
        for (BluetoothScanner.Reading reading : trimmedReadings) {
            totaldBm += reading.dBm;
        }
        return totaldBm / trimmedReadings.size();
    }

    // Implements nearbyDevices() in proximity.vdl.
    @Override
    public ArrayList<Device> nearbyDevices(VContext context, ServerCall serverCall) throws VException {
        Collection<BluetoothScanner.Device> bDevices = scanner.getDevices();
        ArrayList<Device> devices = new ArrayList<Device>();
        for (BluetoothScanner.Device bDevice : bDevices) {
            ArrayList<String> names = new ArrayList<String>();
            names.add(bDevice.device.getName());
            devices.add(new Device(computeAverageDBm(bDevice.readings),
                    names,
                    bDevice.device.getAddress()));
        }
        Collections.sort(devices, new Comparator<Device>() {
            @Override
            public int compare(Device d1, Device d2) {
                if (d1 == null || d2 == null) {
                    return 0;
                }
                return (int)(d2.getDistance() - d1.getDistance());
            }
        });
        return devices;
    }

    public void pause() {
        scanner.pause();
    }

    public void resume() {
        scanner.resume();
    }

    public static class BluetoothNotEnabledException extends Exception {
        private static final long serialVersionUID = -6246827253767202652L;

        public BluetoothNotEnabledException(String msg) {
            super(msg);
        }
    }
}
