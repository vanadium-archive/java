// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.debug;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.IOException;

import io.v.android.error.ErrorReporter;
import io.v.android.error.ToastingErrorReporter;
import io.v.v23.android.R;
import lombok.experimental.UtilityClass;

@UtilityClass
public class Debug {
    private static final String TAG = Debug.class.getSimpleName();

    /**
     * Determines whether the top-level apk was built in debug mode. This is preferable to
     * {@code BuildConfig.DEBUG} as that is only available per module.
     */
    public static boolean isApkDebug(final Context context) {
        return (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
    }

    /**
     * Clears all data for this app. This will force the application to close. It would be nice to
     * be able to schedule a restart as well, but clearing app data clears any scheduled intents.
     * <p>
     * When debugging Vanadium applications, this is often useful to clear the app blessings cache
     * and Syncbase data.
     *
     * @param errorReporter the error reporter used to report any errors. If null,
     *                      {@link ToastingErrorReporter#reportError(Context, int, Throwable)} is
     *                      used.
     */
    public static void clearAppData(final Context context,
                                    @Nullable final ErrorReporter errorReporter) {
        try {
            Runtime.getRuntime().exec("pm clear " + context.getPackageName());
        } catch (final IOException e) {
            if (errorReporter == null) {
                ToastingErrorReporter.reportError(context, R.string.err_app_clear, e);
            } else {
                errorReporter.onError(R.string.err_app_clear, e);
            }
        }
    }

    public static void stopPackageServices(final Context context) {
        final PackageInfo pkg;
        try {
            pkg = context.getPackageManager().getPackageInfo(context.getPackageName(),
                    PackageManager.GET_SERVICES);
        } catch (final PackageManager.NameNotFoundException e) {
            Log.w(TAG, "Unable to enumerate package components", e);
            return;
        }

        for (final ServiceInfo svc : pkg.services) {
            context.stopService(new Intent().setClassName(context, svc.name));
        }
    }

    /**
     * Terminates the JVM for this app. When debugging Vanadium applications, this is useful for
     * terminating any long-lived Android services that might be attached to the app process
     * (e.g. Syncbase).
     * <p>
     * This additionally stops all services defined in the (merged) package manifest before
     * terminating the process. Services default to {@link android.app.Service#START_STICKY}/{@link
     * android.app.Service#START_STICKY_COMPATIBILITY}, which would result in started services
     * auto-restarting after process termination. This could lead to unconventional initialization
     * order, which is not something we necessarily want to exercise while debugging.
     */
    public static void killProcess(final Context context) {
        stopPackageServices(context);
        System.exit(0);
    }

    public static void restartProcess(final Context context) {
        final Intent i = context.getPackageManager().getLaunchIntentForPackage(
                context.getPackageName());
        context.startActivity(i);
        killProcess(context);
    }
}
