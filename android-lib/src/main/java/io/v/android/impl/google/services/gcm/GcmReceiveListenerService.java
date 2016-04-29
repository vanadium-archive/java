// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.impl.google.services.gcm;

import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;

import io.v.v23.verror.VException;

/**
 * Listens for GCM messages and wakes up services upon their receipt.
 */
public class GcmReceiveListenerService extends GcmListenerService {
    private static final String TAG = "GcmRecvListenerService";

    @Override
    public void onMessageReceived(String from, Bundle data) {
        try {
            Util.wakeupServices(this, false);
        } catch (VException e) {
            Log.e(TAG, "Couldn't wakeup services.", e);
        }
    }
}