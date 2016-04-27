// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.impl.google.services.gcm;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;

/**
 * Utility GCM methods.
 */
public class Util {
    /**
     * Returns {@code true} iff the provided service is <em>persistent</em>, i.e., if it
     * can respond to Vanadium RPC requests even when the starting activity has been destroyed.
     *
     * @param context     Android context representing the service
     * @return            {@code true} iff the provided service is <em>persistent</em>
     */
    public static boolean isServicePersistent(Context context) {
        if (!(context instanceof Service)) {
            return false;
        }
        Service service = (Service) context;
        return GcmRegistrationService.isServiceRegistered(context,
                new ComponentName(service, service.getClass()));
    }

    private Util() {}
}
