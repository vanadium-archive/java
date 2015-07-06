// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.location;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import io.v.v23.android.V;
import io.v.v23.context.VContext;
import io.v.v23.naming.Endpoint;
import io.v.v23.rpc.Server;
import io.v.v23.rpc.ServerCall;
import io.v.v23.security.Blessings;
import io.v.v23.security.VPrincipal;
import io.v.v23.security.VSecurity;
import io.v.v23.verror.VException;
import io.v.v23.vom.VomUtil;

public class LocationService extends Service {
    private static final String TAG = "LocationService";

    VContext mBaseContext;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mBaseContext = V.init(this);
        String blessingsVom = intent.getStringExtra(LocationActivity.BLESSINGS_KEY);
        if (blessingsVom == null || blessingsVom.isEmpty()) {
            String msg = "Couldn't start LocationService: null or empty encoded blessings.";
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            android.util.Log.i(TAG, msg);
            return START_STICKY;
        }
        try {
            Blessings blessings =
                    (Blessings) VomUtil.decodeFromString(blessingsVom, Blessings.class);
            if (blessings == null) {
                String msg = "Couldn't start LocationService: null blessings.";
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                android.util.Log.i(TAG, msg);
                return START_STICKY;
            }
            startLocationService(blessings);
        } catch (VException e) {
            String msg = "Couldn't start LocationService: " + e.getMessage();
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            android.util.Log.i(TAG, msg);
        }
        return START_STICKY;
    }

    private void startLocationService(Blessings blessings) {
        try {
            // Update vanadium state with the new blessings.
            VPrincipal p = V.getPrincipal(mBaseContext);
            p.blessingStore().setDefaultBlessings(blessings);
            p.addToRoots(blessings);
            Log.i(TAG, "Added " + blessings + " to root");
//            Log.i(TAG, p.blessingStore().debugString());
            Server s = V.newServer(mBaseContext);
            Endpoint[] endpoints = s.listen(V.getListenSpec(mBaseContext));
            Log.i(TAG, "Listening on endpoint: " + endpoints[0]);
            VeyronLocationService server = new VeyronLocationService(
                    (LocationManager) getSystemService(Context.LOCATION_SERVICE));
            s.serve("spetrovic/location", server, VSecurity.newAllowEveryoneAuthorizer());
            Toast.makeText(this, "Success", Toast.LENGTH_SHORT).show();
        } catch (VException e) {
            String msg = "Couldn't start LocationService: " + e.getMessage();
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            android.util.Log.i(TAG, msg);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        // Binding not allowed.
        return null;
    }

    private class VeyronLocationService implements LocationServer  {
        private final LocationManager manager;

        private VeyronLocationService(LocationManager manager) {
            this.manager = manager;
        }
        @Override
        public LatLng get(VContext context, ServerCall call) throws VException {
            Criteria criteria = new Criteria();
            criteria.setAccuracy(Criteria.NO_REQUIREMENT);
            String provider = manager.getBestProvider(criteria, true);
            if (provider == null || provider.isEmpty()) {
                throw new VException("Couldn't find any location providers on the device.");
            }
            Location location = manager.getLastKnownLocation(provider);
            if (location == null) {
                throw new VException("Got null location.");
            }
            return new LatLng(location.getLatitude(), location.getLongitude());
        }
    }
}
