// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.examples.proximitytest;

import java.util.ArrayList;
import java.util.List;

import io.v.v23.V;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import io.v.examples.proximitytest.R;
import io.v.v23.context.VContext;
import io.v.v23.verror.VException;

/**
 * ProximityTestActivity monitors the list of nearby bluetooth devices and
 * displays various debugging information for each device.
 */
public class ProximityTestActivity extends Activity {
    private static final int REFRESH_PERIOD = 150; // ms

    private ProximityScannerAndroidService serv;
    private PauseHandler handler = new PauseHandler();
    private VContext ctx;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_proxmity_test);
        ctx = V.init();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(getApplicationContext(), ProximityScannerAndroidService.class);
        startService(intent);
        boolean success = bindService(intent, conn, Context.BIND_AUTO_CREATE);
        if (!success) {
            Log.e("ProximityTestActivity", "Failed to bind to service");
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService(conn);
        if (isFinishing()) {
            Intent intent = new Intent(getApplicationContext(),
                    ProximityScannerAndroidService.class);
            stopService(intent);
        }
    }

    private final ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            ProximityScannerAndroidService.BluetoothTestBinder binder =
                    (ProximityScannerAndroidService.BluetoothTestBinder) service;
            serv = binder.getService();
            try {
                serv.start();
            } catch (ProximityScannerVeyronService.BluetoothNotEnabledException e) {
                Log.i("ProximityTestActivity", "Starting proximity test failed because bluetooth is not enabled.");
                Toast.makeText(getApplicationContext(), "Bluetooth must be enabled. Please renable it and restart the app.", Toast.LENGTH_SHORT).show();
                return;
            } catch(VException e) {
                Toast.makeText(getApplicationContext(), "VeyronException while starting service: " + e, Toast.LENGTH_SHORT).show();
                return;
            }
            handler = new PauseHandler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    refreshDevices();
                    handler.postDelayed(this, REFRESH_PERIOD);
                }
            }, REFRESH_PERIOD);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            serv = null;
        }
    };

    public List<Device> fetchDevices() {
        String endpoint = "/" + serv.endpoints[0];
        ProximityScannerClient ps = ProximityScannerClientFactory.getProximityScannerClient(endpoint);
        try {
            List<Device> devices = ps.nearbyDevices(ctx);
            if (devices == null) {
                // TODO(bprosnitz) Remove this. This is a temporary hack because
                // VDL decodes empty slices into nil.
                devices = new ArrayList<Device>();
            }
            return devices;
        } catch (VException e) {
            Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show();
            ;
            return new ArrayList<Device>();
        }
    }

    public void refreshDevices() {
        List<Device> devices = fetchDevices();
        // Update the devices view.
        final ListView devicesView = (ListView) findViewById(R.id.devices);
        devicesView.setAdapter(new DeviceAdapter(this, new ArrayList<Device>(devices)));
    }

    private class DeviceAdapter extends BaseAdapter {
        private final Context context;
        private final ArrayList<Device> devices;

        DeviceAdapter(Context context, ArrayList<Device> devices) {
            this.context = context;
            this.devices = devices;
        }

        @Override
        public int getCount() {
            if (devices == null) {
                return 0;
            }
            return devices.size();
        }

        @Override
        public Object getItem(int position) {
            if (devices != null && position >= 0 && position < devices.size()) {
                return devices.get(position);
            }
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView textView;
            if (convertView == null) {
                // If it's not recycled, create a new one.
                textView = new TextView(context);
                textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
            } else {
                textView = (TextView) convertView;
            }

            final Device device = (Device) getItem(position);
            if (device != null) {
                textView.setText(String.format("distance: %d names: %s, mac: %s", device.getDistance(), device.getNames(), device.getMac()));
                if (Math.abs(device.getDistance()) < 50) {
                    textView.setTextColor(Color.GREEN);
                } else {
                    textView.setTextColor(Color.BLACK);
                }
            } else {
                textView.setText("UNKNOWN DEVICE");
                textView.setTextColor(Color.RED);
            }
            return textView;
        }
    }
}
