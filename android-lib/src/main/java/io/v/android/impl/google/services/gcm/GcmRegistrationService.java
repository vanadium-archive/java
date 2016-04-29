// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.impl.google.services.gcm;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import java.io.IOException;

import io.v.v23.verror.VException;

/**
 * Communicates with GCM servers to obtain a new registration token and then starts
 * all app services that have registered themselves as wake-able.
 */
public class GcmRegistrationService extends IntentService {
    private static final String TAG = "GcmRegistrationService";
    public static final String GCM_TOKEN_PREF_KEY = "io.v.android.impl.google.services.gcm.TOKEN";
    static final String EXTRA_RESTART_SERVICES = "RESTART_SERVICES";

    public GcmRegistrationService() {
        super("GcmRegistrationService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            InstanceID instanceID = InstanceID.getInstance(this);
            String token = instanceID.getToken("*", GoogleCloudMessaging.INSTANCE_ID_SCOPE);
            // Store registration token in SharedPreferences.
            SharedPreferences.Editor editor =
                    PreferenceManager.getDefaultSharedPreferences(this).edit();
            editor.putString(GCM_TOKEN_PREF_KEY, token);
            editor.commit();
        } catch (IOException e) {
            Log.e(TAG, "Couldn't fetch GCM registration token: ", e);
        }
        boolean restartServices = intent.getBooleanExtra(EXTRA_RESTART_SERVICES, false);
        try {
            Util.wakeupServices(this, restartServices);
        } catch (VException e) {
            Log.e(TAG, "Couldn't wakeup services.", e);
        }
    }
}