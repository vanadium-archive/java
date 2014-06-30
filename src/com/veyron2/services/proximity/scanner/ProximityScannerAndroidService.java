package com.veyron2.services.proximity.scanner;

import android.app.Service;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import com.veyron2.RuntimeFactory;
import com.veyron2.ipc.Dispatcher;
import com.veyron2.ipc.VeyronException;
import com.veyron2.services.proximity.vdlgen.Server;

public class ProximityScannerAndroidService extends Service {
    private ProximityScannerVeyronService proxService;
    private com.veyron2.ipc.Server s;
    public String endpoint;

    public class BluetoothTestBinder extends Binder {
        public ProximityScannerAndroidService getService() {
            return ProximityScannerAndroidService.this;
        }
    }

    @Override
    public void onCreate() {
        try {
            s = RuntimeFactory.getRuntime().newServer();
            proxService = ProximityScannerVeyronService
                    .create((BluetoothManager) getSystemService(BLUETOOTH_SERVICE));
            final Object stub = Server.newProximityScanner(proxService);
            s.register("", new Dispatcher() {
                @Override
                public Object lookup(String suffix) throws VeyronException {
                    return stub;
                }
            });
            endpoint = s.listen("tcp", "127.0.0.1:8100");
            s.publish("fortune");
        } catch (VeyronException e) {
            throw new RuntimeException(
                    "Exception during ProximityScannerAndroidService onCreate()", e);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new BluetoothTestBinder();
    }

    @Override
    public void onDestroy() {
        try {
            s.stop();
        } catch (VeyronException e) {
            throw new RuntimeException("Exception stopping service", e);
        }
        super.onDestroy();
    }
}
