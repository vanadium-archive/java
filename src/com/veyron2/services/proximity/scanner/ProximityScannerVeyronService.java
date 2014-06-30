package com.veyron2.services.proximity.scanner;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;

import com.veyron2.ipc.ServerContext;
import com.veyron2.ipc.VeyronException;
import com.veyron2.services.proximity.Device;
import com.veyron2.services.proximity.ProximityScannerService;

public class ProximityScannerVeyronService implements ProximityScannerService {

    private final BluetoothScanner scanner;

    public ProximityScannerVeyronService(BluetoothScanner scanner) {
        this.scanner = scanner;
    }

    public static ProximityScannerVeyronService create(final BluetoothManager bluetoothManager) {
        final BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            throw new RuntimeException("Bluetooth not enabled");
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
            // 'readings',
            // which leaves at least ceil(readings.size() / 3) elements, which
            // is greater than 0 when readings.size() > 0.
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
    public ArrayList<Device> nearbyDevices(ServerContext context) throws VeyronException {
        Collection<BluetoothScanner.Device> bDevices = scanner.getDevices();
        ArrayList<Device> devices = new ArrayList<Device>();
        for (BluetoothScanner.Device bDevice : bDevices) {
            Device d = new Device();
            d.distance = Integer.toString(computeAverageDBm(bDevice.readings));
            d.mAC = bDevice.device.getAddress();
            d.names = new ArrayList<String>();
            d.names.add(bDevice.device.getName());
            devices.add(d);
        }
        Collections.sort(devices, new Comparator<Device>() {
            @Override
            public int compare(Device d1, Device d2) {
                if (d1 == null || d1.distance == null || d2 == null || d2.distance == null) {
                    return 0;
                }
                return Integer.parseInt(d2.distance) - Integer.parseInt(d1.distance);
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
}
