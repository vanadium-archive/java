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
import io.v.v23.security.Security;
import io.v.v23.verror.VException;

public class LocationService extends Service {
    private static final String TAG = "io.veyron.location";

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
        final VContext ctx = V.init(this);
        try {
            final Server s = V.newServer(ctx);
            final String[] endpoints = s.listen(null);
            Log.i(TAG, "Listening on endpoint: " + endpoints[0]);
            final VeyronLocationService server =
                    new VeyronLocationService((LocationManager) getSystemService(Context.LOCATION_SERVICE));
            final Dispatcher dispatcher = new Dispatcher() {
                @Override
                public ServiceObjectWithAuthorizer lookup(String suffix) throws VException {
                    return new ServiceObjectWithAuthorizer(server, Security.newAcceptAllAuthorizer());
                }
            };
            s.serve("spetrovic/location", dispatcher);
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
        public LatLng get(ServerCall call) throws VException {
            final Criteria criteria = new Criteria();
            criteria.setAccuracy(Criteria.NO_REQUIREMENT);
            final String provider = manager.getBestProvider(criteria, true);
            if (provider == null || provider.isEmpty()) {
                throw new VException("Couldn't find any location providers on the device.");
            }
            Log.i(TAG, "Using location provider: " + provider);
            final Location location = manager.getLastKnownLocation(provider);
            if (location == null) {
                throw new VException("Got null location.");
            }
            return new LatLng(location.getLatitude(), location.getLongitude());
        }
    }
}
