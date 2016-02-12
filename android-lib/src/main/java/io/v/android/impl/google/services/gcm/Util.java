// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.impl.google.services.gcm;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;

import java.util.ArrayList;
import java.util.List;

import io.v.v23.verror.VException;

/**
 * Utility GCM methods.
 */
class Util {
    // Wakes up all services that have marked themselves for wakeup.
    // If restart==true, services are first stopped and then started;  otherwise, they
    // are just started.
    static void wakeupServices(Context context, boolean restart) throws VException {
        for (ServiceInfo service : getWakeableServices(context)) {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName(service.packageName, service.name));
            if (restart) {
                context.stopService(intent);
            }
            context.startService(intent);
        }
    }

    // Returns a list of all services that have marked themselves for wakeup.
    static List<ServiceInfo> getWakeableServices(Context context) throws VException {
        try {
            ServiceInfo[] services = context.getPackageManager().getPackageInfo(
                    context.getPackageName(),
                    PackageManager.GET_META_DATA|PackageManager.GET_SERVICES).services;
            if (services == null) {
                throw new VException("Couldn't get services information for package: " +
                        context.getPackageName());
            }
            ArrayList<ServiceInfo> ret = new ArrayList<>();
            for (ServiceInfo service : services) {
                if (service == null) continue;
                if (!service.packageName.equals(context.getPackageName())) continue;
                if (service.metaData == null) continue;
                boolean wakeup = service.metaData.getBoolean("wakeup", false);
                if (!wakeup) continue;
                ret.add(service);
            }
            return ret;
        } catch (PackageManager.NameNotFoundException e) {
            throw new VException(String.format(
                    "Couldn't get package information for package %s: %s",
                    context.getPackageName(), e.getMessage()));
        }
    }

    /**
     * Returns {@code true} iff the provided service is "wakeable", i.e., if it has the
     * {@code "wakeup"} metadata attached to it.
     *
     * @param context     Android context representing the service
     * @return            {@code true} iff the provided service is "wakeable"
     * @throws VException if there was an error figuring out if a service is wakeable
     */
    public static boolean isServiceWakeable(Context context) throws VException {
        if (!(context instanceof Service)) {
            return false;
        }
        Service service = (Service) context;
        try {
            ServiceInfo info = service.getPackageManager().getServiceInfo(
                    new ComponentName(service, service.getClass()), PackageManager.GET_META_DATA);
            if (info.metaData == null) {
                return false;
            }
            return info.metaData.getBoolean("wakeup", false);
        } catch (PackageManager.NameNotFoundException e) {
            throw new VException(e.getMessage());
        }
    }

    private Util() {}
}
