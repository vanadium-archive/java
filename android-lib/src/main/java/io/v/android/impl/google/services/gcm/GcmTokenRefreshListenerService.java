// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.impl.google.services.gcm;

import android.content.Intent;

import com.google.android.gms.iid.InstanceIDListenerService;

/**
 * Listens for server token change notifications and initiates token (and services) refresh.
 */
public class GcmTokenRefreshListenerService extends InstanceIDListenerService {
    @Override
    public void onTokenRefresh() {
        Intent intent = new Intent(this, GcmRegistrationService.class);
        intent.putExtra(GcmRegistrationService.EXTRA_RESTART_SERVICES, true);
        startService(intent);
    }
}