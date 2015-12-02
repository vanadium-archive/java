// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;

import java.io.IOException;

import io.v.android.v23.V;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public class DebugUtils {
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
     */
    public static void clearAppData(final Context context) {
        try {
            Runtime.getRuntime().exec("pm clear " + context.getPackageName());
        } catch (final IOException e) {
            if (context instanceof VAndroidContextMixin) {
                ((VAndroidContextMixin) context).getVAndroidContextTrait()
                        .getErrorReporter()
                        .onError(R.string.err_app_clear, e);
            } else {
                log.error("Unable to clear app data", e);
            }
        }
    }

    public static void stopPackageServices(final Context context) {
        final PackageInfo pkg;
        try {
            pkg = context.getPackageManager().getPackageInfo(context.getPackageName(),
                    PackageManager.GET_SERVICES);
        } catch (final PackageManager.NameNotFoundException e) {
            log.warn("Unable to enumerate package components", e);
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
        V.shutdown();
        System.exit(0);
    }

    public static void restartProcess(final Context context) {
        final Intent i = context.getPackageManager().getLaunchIntentForPackage(
                context.getPackageName());
        context.startActivity(i);
        killProcess(context);
    }
}
