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

import io.v.v23.android.V;
import io.v.v23.context.VContext;
import io.v.v23.rpc.Dispatcher;
import io.v.v23.rpc.Server;
import io.v.v23.rpc.ServerCall;
import io.v.v23.rpc.ServiceObjectWithAuthorizer;
import io.v.v23.security.VSecurity;
import io.v.v23.verror.VException;

public class LocationService extends Service {
    private static final String TAG = "LocationService";

    private volatile boolean mStarted = false;
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mStarted) return START_STICKY;
        synchronized (this) {
            if (mStarted) return START_STICKY;
            mStarted = true;
            startLocationService();
        }
        return START_STICKY;
    }

    private void startLocationService() {
        VContext ctx = V.init(this);
        try {
            Server s = V.newServer(ctx);
            String[] endpoints = s.listen(V.getListenSpec(ctx));
            Log.i(TAG, "Listening on endpoint: " + endpoints[0]);
            VeyronLocationService server = new VeyronLocationService(
                    (LocationManager) getSystemService(Context.LOCATION_SERVICE));
            s.serve("spetrovic/location", server, VSecurity.newAllowEveryoneAuthorizer());
        } catch (VException e) {
            Log.e(TAG, "Couldn't start location service: " + e.getMessage());
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
