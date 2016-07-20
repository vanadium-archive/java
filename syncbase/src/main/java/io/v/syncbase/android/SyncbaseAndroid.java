// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase.android;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Context;

import java.util.UUID;

import io.v.syncbase.Syncbase;
import io.v.syncbase.User;

/**
 * Contains helper methods for initializing Syncbase on Android.
 */
public final class SyncbaseAndroid {
    private SyncbaseAndroid() {}

    /**
     * Logs in the user on Android.
     * If the user is already logged in, it runs the success callback on the executor. Otherwise,
     * the user selects an account through an account picker flow and is logged into Syncbase. The
     * callback's success or failure cases are called accordingly.
     * Note: This default account flow is currently restricted to Google accounts.
     *
     * @param activity The Android activity where login will occur.
     * @param callback The callback to call when the login was done.
     */
    public static void login(Activity activity, final Syncbase.LoginCallback callback) {
        if (Syncbase.isLoggedIn()) {
            Syncbase.executeCallback(new Runnable() {
                @Override
                public void run() {
                    callback.onSuccess();
                }
            });
            return;
        }
        FragmentTransaction transaction = activity.getFragmentManager().beginTransaction();
        LoginFragment fragment = new LoginFragment();
        fragment.setTokenReceiver(new LoginFragment.TokenReceiver() {
            @Override
            public void receiveToken(String token) {
                Syncbase.login(token, User.PROVIDER_GOOGLE, callback);
            }
        });
        transaction.add(fragment, UUID.randomUUID().toString());
        transaction.commit(); // This will invoke the fragment's onCreate() immediately.
    }

    /**
     * Computes a default location to store Syncbase data.
     *
     * @param context The Android context
     */
    public static String defaultRootDir(Context context) {
        return context.getDir("syncbase", Context.MODE_PRIVATE).getAbsolutePath();
    }

    // TODO(alexfandrianto): Add options builders for Android. We would like to specify the default
    // root dir as well as the mount point if possible. A good default mount point could be computed
    // off the app id; unfortunately, we won't know that until after we've logged in.
}
