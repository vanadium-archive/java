// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

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
