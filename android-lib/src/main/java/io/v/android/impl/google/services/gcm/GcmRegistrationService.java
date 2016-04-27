// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.impl.google.services.gcm;

import android.app.IntentService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import org.joda.time.Duration;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import io.v.android.v23.V;
import io.v.v23.VFutures;
import io.v.v23.context.VContext;
import io.v.v23.services.wakeup.WakeUpClient;
import io.v.v23.services.wakeup.WakeUpClientFactory;
import io.v.v23.verror.VException;

/**
 * Communicates with GCM servers to obtain a new registration token and then controls
 * all app services that have registered themselves as persistent.
 */
public class GcmRegistrationService extends IntentService {
    /**
     * A key in {@link SharedPreferences} under which the mount root for
     * app's persistent services is stored.
     */
    public static final String WAKEUP_MOUNT_ROOT_PREF_KEY =
            "io.v.android.impl.google.services.gcm.WAKEUP_MOUNT_ROOT";

    /**
     * Registers the service and starts it (via {@link #startService}).
     * <p>
     * If the registration fails, the service will be started but may never be woken up
     * afterward.
     * <p>
     * If the service has already been registered it will only be started.
     */
    public static void registerAndStartService(Context ctx, ComponentName service) {
        Intent intent = new Intent(ctx, GcmRegistrationService.class);
        intent.putExtra(EXTRA_MODE, Mode.REGISTER_AND_START_SERVICE.getValue());
        intent.putExtra(EXTRA_SERVICE_INFO, service.flattenToString());
        ctx.startService(intent);
    }

    /**
     * Unregisters the services and stops it (via {@link #stopService}).
     */
    public static void unregisterAndStopService(Context ctx, ComponentName service) {
        Intent intent = new Intent(ctx, GcmRegistrationService.class);
        intent.putExtra(EXTRA_MODE, Mode.UNREGISTER_AND_STOP_SERVICE.getValue());
        intent.putExtra(EXTRA_SERVICE_INFO, service.flattenToString());
        ctx.startService(intent);
    }

    /**
     * Refreshes the GCM token and restarts all registered services (via {@link #stopService}
     * followed by {@link #startService}.
     */
    public static void refreshTokenAndRestartRegisteredServices(Context ctx) {
        Intent intent = new Intent(ctx, GcmRegistrationService.class);
        intent.putExtra(EXTRA_MODE, Mode.REFRESH_TOKEN_AND_RESTART_REGISTERED_SERVICES.getValue());
        ctx.startService(intent);
    }

    // Stores the set of all persistent services' flattened ComponentNames.
    private static final String REGISTERED_SERVICES_PREF_KEY =
            "io.v.android.impl.google.services.gcm.REGISTERED_SERVICES_PREF_KEY";
    private static final String EXTRA_MODE = "EXTRA_MODE";
    private static final String EXTRA_SERVICE_INFO= "EXTRA_SERVICE_INFO";
    private static final String VANADIUM_WAKEUP_SERVICE =
            "/ns.dev.v.io:8101/users/jenkins.veyron@gmail.com/wakeup/server";
    private static final String TAG = "GcmRegistrationService";

    private enum Mode {
        REGISTER_AND_START_SERVICE (1),
        UNREGISTER_AND_STOP_SERVICE (2),
        REFRESH_TOKEN_AND_RESTART_REGISTERED_SERVICES (3);

        private final int value;
        Mode(int value) {
            this.value = value;
        }

        int getValue() {
            return value;
        }

        static Mode fromInt(int value) {
            for (Mode m : Mode.values()) {
                if (m.getValue() == value) {
                    return m;
                }
            }
            return null;
        }
    }

    public GcmRegistrationService() {
        super("GcmRegistrationService");
    }

    @Override
    protected synchronized void onHandleIntent(Intent intent) {
        Mode mode = Mode.fromInt(intent.getIntExtra(EXTRA_MODE, -1));
        switch (mode) {
            case REGISTER_AND_START_SERVICE:
                registerAndStartService(intent);
                break;
            case UNREGISTER_AND_STOP_SERVICE:
                unregisterAndStopService(intent);
                break;
            case REFRESH_TOKEN_AND_RESTART_REGISTERED_SERVICES:
                refreshTokenAndRestartRegisteredServices();
                break;
            default:
                Log.e(TAG, String.format("Invalid mode %s for GCMRegistrationService. " +
                        "Dropping the request.", mode));
        }
    }

    private void registerAndStartService(Intent intent) {
        ComponentName service = ComponentName.unflattenFromString(
                intent.getStringExtra(EXTRA_SERVICE_INFO));
        if (service == null) {
            Log.e(TAG, "Couldn't extract service information from intent - dropping the request.");
            return;
        }
        registerService(service);
        if (loadMountRoot().isEmpty()) {
            refreshToken();
        }
        startService(service);
    }

    private void unregisterAndStopService(Intent intent) {
        ComponentName service = intent.getParcelableExtra(EXTRA_SERVICE_INFO);
        if (service == null) {
            Log.e(TAG, "Couldn't extract service information from intent - dropping the request.");
            return;
        }
        unregisterService(service);
        stopService(service);
    }

    private void refreshTokenAndRestartRegisteredServices() {
        refreshToken();
        for (ComponentName service : getRegisteredServices(this)) {
            stopService(service);
            startService(service);
        }
    }

    private void refreshToken() {
        VContext ctx = V.init(this);
        try {
            // Get token from Google GCM servers.
            InstanceID instanceID = InstanceID.getInstance(this);
            // TODO(spetrovic): Add a TTL for the token.
            String token = instanceID.getToken("632758215260", GoogleCloudMessaging.INSTANCE_ID_SCOPE);

            Log.d(TAG, "Token is: " + token);

            // Register the token with the Vanadium wakeup service.
            WakeUpClient wake = WakeUpClientFactory.getWakeUpClient(VANADIUM_WAKEUP_SERVICE);
            VContext ctxT = ctx.withTimeout(Duration.standardSeconds(20));
            String mountRoot = VFutures.sync(wake.register(ctxT, token));

            // Store the wakeup mount root in SharedPreferences.
            storeMountRoot(mountRoot);
        } catch (IOException e) {
            Log.e(TAG, "Couldn't fetch GCM registration token: " + e.toString());
        } catch (VException e) {
            Log.e(TAG, "Couldn't register GCM token with vanadium services: " + e.toString());
        } finally {
            ctx.cancel();
        }
    }

    private void registerService(ComponentName service) {
        Set<String> s = loadRegisteredServices(this);
        s.add(service.flattenToString());
        storeRegisteredServices(s);
    }

    private void unregisterService(ComponentName service) {
        Set<String> s = loadRegisteredServices(this);
        s.remove(service.flattenToString());
        storeRegisteredServices(s);
    }

    /**
     * Returns {@code true} iff the given {@code service} is a registered persistent service.
     */
    public static boolean isServiceRegistered(Context context, ComponentName service) {
        Set<String> s = loadRegisteredServices(context);
        return s.contains(service.flattenToString());
    }

    /**
     * Returns a list of all registered persistent services.
     */
    public static ComponentName[] getRegisteredServices(Context context) {
        Set<String> s = loadRegisteredServices(context);
        String[] names = s.toArray(new String[s.size()]);
        ComponentName[] ret = new ComponentName[s.size()];
        for (int i = 0; i < names.length; ++i) {
            ret[i] = ComponentName.unflattenFromString(names[i]);
        }
        return ret;
    }

    private static Set<String> loadRegisteredServices(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getStringSet(
                REGISTERED_SERVICES_PREF_KEY, new HashSet<String>());
    }

    private void storeRegisteredServices(Set<String> services) {
        SharedPreferences.Editor editor =
                PreferenceManager.getDefaultSharedPreferences(this).edit();
        editor.putStringSet(REGISTERED_SERVICES_PREF_KEY, services);
        editor.commit();
    }

    private String loadMountRoot() {
        return PreferenceManager.getDefaultSharedPreferences(this).getString(
                WAKEUP_MOUNT_ROOT_PREF_KEY, "");
    }

    private void storeMountRoot(String mountRoot) {
        SharedPreferences.Editor editor =
                PreferenceManager.getDefaultSharedPreferences(this).edit();
        editor.putString(WAKEUP_MOUNT_ROOT_PREF_KEY, mountRoot);
        editor.commit();
    }

    private void startService(ComponentName service) {
        final Intent intent = new Intent();
        intent.setComponent(service);
        startService(intent);
    }

    private void stopService(ComponentName service) {
        Intent intent = new Intent();
        intent.setComponent(service);
        stopService(intent);
    }
}