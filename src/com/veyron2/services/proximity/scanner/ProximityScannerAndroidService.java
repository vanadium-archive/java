
package com.veyron2.services.proximity.scanner;

import android.app.Service;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.veyron2.RuntimeFactory;
import com.veyron2.ipc.Dispatcher;
import com.veyron2.ipc.VeyronException;
import com.veyron2.services.proximity.scanner.ProximityScannerVeyronService.BluetoothNotEnabledException;

public class ProximityScannerAndroidService extends Service {
    private ProximityScannerVeyronService proxService;
    private com.veyron2.ipc.Server s;
    public String endpoint;

    public class BluetoothTestBinder extends Binder {
        public ProximityScannerAndroidService getService() {
            return ProximityScannerAndroidService.this;
        }
    }

    public void start() throws VeyronException, BluetoothNotEnabledException {
        proxService = ProximityScannerVeyronService
                .create((BluetoothManager) getSystemService(BLUETOOTH_SERVICE));
        s = RuntimeFactory.getRuntime().newServer();
        endpoint = s.listen("tcp", "127.0.0.1:8100");
        s.serve("proximity", new Dispatcher() {
            @Override
            public Object lookup(String suffix) throws VeyronException {
                return proxService;
            }
        });
    }

    private void stop() {
        try {
            if (s != null) {
                s.stop();
            }
        } catch (VeyronException e) {
            // We don't expect this exception to occur.
            Log.e("ProxmityScannerAndroidService", "Failed to stop veyron service: " + e);
        }
        endpoint = null;
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
