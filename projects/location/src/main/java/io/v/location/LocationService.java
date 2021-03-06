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

import com.google.common.collect.Lists;

import java.util.Arrays;
import java.util.List;

import io.v.android.v23.V;
import io.v.v23.context.VContext;
import io.v.v23.rpc.ListenSpec;
import io.v.v23.rpc.Server;
import io.v.v23.rpc.ServerCall;
import io.v.v23.security.BlessingPattern;
import io.v.v23.security.Blessings;
import io.v.v23.security.VCertificate;
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
            Log.i(TAG, msg);
            return START_REDELIVER_INTENT;
        }
        try {
            Blessings blessings =
                    (Blessings) VomUtil.decodeFromString(blessingsVom, Blessings.class);
            if (blessings == null) {
                String msg = "Couldn't start LocationService: null blessings.";
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                Log.i(TAG, msg);
                return START_REDELIVER_INTENT;
            }
            storeBlessings(blessings);
            startLocationService(determineMountPoint(blessings));
        } catch (VException e) {
            String msg = "Couldn't start LocationService: " + e.getMessage();
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            Log.i(TAG, msg);
        }
        return START_REDELIVER_INTENT;
    }

    private void startLocationService(String mountPoint) throws VException {
        VeyronLocationService locationService = new VeyronLocationService(
                (LocationManager) getSystemService(Context.LOCATION_SERVICE));
        ListenSpec spec = V.getListenSpec(mBaseContext).withProxy("proxy");
        Log.i(TAG, "Mounting server at " + mountPoint);
        VContext ctx = V.withNewServer(
                V.withListenSpec(mBaseContext, spec), mountPoint, locationService, null);
        Server server = V.getServer(ctx);
        Log.i(TAG, "Listening on endpoints: " + Arrays.toString(
                server.getStatus().getEndpoints()));
        Toast.makeText(this, "Success", Toast.LENGTH_SHORT).show();
    }

    // Update vanadium state with the new blessings.
    private void storeBlessings(Blessings blessings) throws VException {
        VPrincipal p = V.getPrincipal(mBaseContext);
        p.blessingStore().setDefaultBlessings(blessings);
        p.blessingStore().set(blessings, new BlessingPattern("..."));
        VSecurity.addToRoots(p, blessings);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // Binding not allowed.
        return null;
    }

    private static class VeyronLocationService implements LocationServer {
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

    private static String determineMountPoint(Blessings blessings) {
        String name = userEmailFromBlessings(blessings);
        return name.isEmpty() ? "" : "users/" + name + "/location";
    }

    private static String userEmailFromBlessings(Blessings blessings) {
        for (List<VCertificate> chain : blessings.getCertificateChains()) {
            for (VCertificate certificate : Lists.reverse(chain)) {
                if (certificate.getExtension().contains("@")) {
                    return certificate.getExtension();
                }
            }
        }
        Log.w(TAG, "Could not determine name from blessings '" + blessings + "'");
        return "";
    }
}
