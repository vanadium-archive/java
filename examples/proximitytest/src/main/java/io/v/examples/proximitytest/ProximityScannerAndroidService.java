// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.


package io.v.examples.proximitytest;

import android.app.Service;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import io.v.v23.V;
import io.v.v23.context.VContext;
import io.v.v23.naming.Endpoint;
import io.v.v23.rpc.AddressChooser;
import io.v.v23.rpc.Dispatcher;
import io.v.v23.rpc.ListenSpec;
import io.v.v23.rpc.Server;
import io.v.v23.rpc.ServiceObjectWithAuthorizer;
import io.v.v23.security.Authorizer;
import io.v.v23.security.Call;
import io.v.v23.verror.VException;

public class ProximityScannerAndroidService extends Service {
    private ProximityScannerVeyronService proxService;
    private Server s;
    public Endpoint[] endpoints;

    public class BluetoothTestBinder extends Binder {
        public ProximityScannerAndroidService getService() {
            return ProximityScannerAndroidService.this;
        }
    }

    public void start() throws VException, ProximityScannerVeyronService.BluetoothNotEnabledException {
        proxService = ProximityScannerVeyronService
                .create((BluetoothManager) getSystemService(BLUETOOTH_SERVICE));
        VContext ctx = V.init();
        s = V.newServer(ctx);
        endpoints = s.listen(V.getListenSpec(ctx));
        s.serve("proximity", proxService, new Authorizer() {
            @Override
            public void authorize(VContext ctx, Call call) throws VException {
                // always authorize
            }
        });
    }

    private void stop() {
        try {
            if (s != null) {
                s.stop();
            }
        } catch (VException e) {
            // We don't expect this exception to occur.
            Log.e("ProximityScannerAndroidService", "Failed to stop veyron service: " + e);
        }
        endpoints = null;
        proxService = null;
        s = null;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new BluetoothTestBinder();
    }

    @Override
    public void onDestroy() {
        stop();
        super.onDestroy();
    }
}
