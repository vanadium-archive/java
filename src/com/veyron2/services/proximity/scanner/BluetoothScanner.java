package com.veyron2.services.proximity.scanner;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.util.Log;

import com.google.common.collect.ImmutableList;
import com.veyron2.PauseHandler;

/**
 * BluetoothScanner performs periodic LE scans and collects information about
 * nearby devices, such as their advertised names and signal power levels.
 */
class BluetoothScanner implements BluetoothAdapter.LeScanCallback {
    public static final int BT_SCAN_PERIOD = 50; // ms
    // Keep history of these many previous scan results.
    private static final int BT_SCAN_HISTORY = 100; // # of scan results

    private final BluetoothAdapter adapter;
    // A list of Bluetooth readings for all devices encountered thus far,
    // keyed by device address.
    private final ConcurrentHashMap<String, Device> devices;
    private final PauseHandler handler;
    long scanIteration;

    /**
     * Construct the bluetooth scanner.
     *
     * @param btAdapter
     */
    public BluetoothScanner(BluetoothAdapter btAdapter) {
        this.adapter = btAdapter;
        devices = new ConcurrentHashMap<String, Device>();
        handler = new PauseHandler();
        if (!adapter.startLeScan(this)) {
            Log.e("BluetoothScanner", "Failed to start LE scan.");
        }
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                adapter.stopLeScan(BluetoothScanner.this);
                updateState();
                if (!adapter.startLeScan(BluetoothScanner.this)) {
                    Log.e("BluetoothScanner", "Failed to start LE scan.");
                }
                handler.postDelayed(this, BT_SCAN_PERIOD);
            }
        }, BT_SCAN_PERIOD);
    }

    // Pauses scanning.
    void pause() {
        handler.pause();
        adapter.stopLeScan(this);
    }

    // Resumes scanning.
    void resume() {
        adapter.startLeScan(this);
        handler.resume();
    }

    // Returns the list of all devices for which we have recent readings, along
    // with those readings.
    public Collection<Device> getDevices() {
        Collection<Device> ds = devices.values();
        ImmutableList.Builder<Device> builder = new ImmutableList.Builder<Device>();
        for (Device d : ds) {
            synchronized (d) {
                ImmutableList.Builder<Reading> readingsBuilder = new ImmutableList.Builder<Reading>();
                for (Reading r : d.readings) {
                    readingsBuilder.add(r);
                }
                builder.add(new Device(d.device, readingsBuilder.build()));
            }
        }
        return builder.build();
    }

    private void updateState() {
        Log.d("BluetoothScanner", "Updating state");
        long curIteration = -1;
        synchronized (this) {
            curIteration = scanIteration++;
        }
        // Cleanup stale entries.
        final long oldestIteration = curIteration - BT_SCAN_HISTORY;
        for (Iterator<Map.Entry<String, Device>> it = this.devices.entrySet().iterator(); it
                .hasNext();) {
            final Device device = it.next().getValue();
            synchronized (device) {
                final List<Reading> readings = device.readings;
                // Instead of scanning all of device's readings to find the ones
                // that
                // are too stale, we simply limit the number of readings to
                // O(BT_SCAN_HISTORY) in order to improve performance.
                if (readings.size() > 2 * BT_SCAN_HISTORY) {
                    readings.subList(0, readings.size() - BT_SCAN_HISTORY).clear();
                }
                // If the freshest reading is too stale, purge the device from
                // the list.
                if (readings.isEmpty()
                        || readings.get(readings.size() - 1).iteration < oldestIteration) {
                    it.remove();
                }
            }
        }
    }

    @Override
    public void onLeScan(BluetoothDevice btDevice, int rssi, byte[] scanRecord) {
        Log.d("BluetoothScanner", "Le scan " + btDevice.getName() + " " + Integer.toString(rssi));
        long curIteration = -1;
        synchronized (this) {
            curIteration = scanIteration;
        }
        Device device = devices.get(btDevice.getAddress());
        if (device == null) {
            device = new Device(btDevice);
            devices.put(btDevice.getAddress(), device);
        }
        synchronized (device) {
            device.readings.add(new Reading(System.currentTimeMillis(), curIteration, rssi));
        }
    }

    class Device {
        BluetoothDevice device;
        List<Reading> readings;

        Device(BluetoothDevice device) {
            this.device = device;
            this.readings = new ArrayList<Reading>();
        }

        Device(BluetoothDevice device, List<Reading> readings) {
            this.device = device;
            this.readings = readings;
        }
    }

    class Reading {
        final long time;
        final long iteration;
        final int dBm;

        Reading(long time, long iteration, int dBm) {
            this.time = time;
            this.iteration = iteration;
            this.dBm = dBm;
        }
    }
}