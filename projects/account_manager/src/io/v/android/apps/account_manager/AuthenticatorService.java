package io.v.android.apps.account_manager;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Adapter class for making Authenticator available as a Service.
 */
public class AuthenticatorService extends Service {
  @Override
  public IBinder onBind(Intent intent) {
    return new Authenticator(this).getIBinder();
  }
}
