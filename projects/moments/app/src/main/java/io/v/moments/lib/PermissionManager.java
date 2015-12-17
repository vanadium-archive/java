// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.moments.lib;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;

/**
 * Testable helper class for obtaining permission and minimizing boilerplate in
 * activities that simply exit if they don't get all permissions (e.g. demo
 * apps). See https://developer.android.com/training/permissions/requesting.html
 *
 * Usage in an activity:
 *
 * public class Activity extends ... {
 *
 * private static final String[] PERMS = { Manifest.permission.FOO,
 * Manifest.permission.BAR };
 *
 * private PermissionManager mPermissionManager = new PermissionManager(this,
 * RequestCode.PERMISSIONS, PERMS);
 *
 * @Override protected void onCreate(Bundle b) { super.onCreate(b); ...;
 * mPermissionManager.obtainPermission(); .... }
 * @Override public void onRequestPermissionsResult( int requestCode, String[]
 * permissions, int[] results) { if (!mPermissionManager.granted( requestCode,
 * permissions, results)) { Toast.makeText(this, "Need all permissions.",
 * Toast.LENGTH_LONG); finishActivity(); } }
 *
 * }
 */
public class PermissionManager {
    private final static String TAG = "PermissionManager";

    private final Activity mActivity;
    private final int mRequestCode;
    private final String[] mPerms;
    private final int mAndroidLevel;
    private boolean mIsRequestInProgress;

    PermissionManager(int androidLevel, Activity activity, int requestCode, String[] perms) {
        mAndroidLevel = androidLevel;
        mActivity = activity;
        mRequestCode = requestCode;
        mPerms = perms;
    }

    public PermissionManager(Activity activity, int requestCode, String[] perms) {
        this(Build.VERSION.SDK_INT, activity, requestCode, perms);
    }

    /**
     * This helps to avoid stacking requests for permissions.
     */
    public synchronized boolean isRequestInProgress() {
        return mIsRequestInProgress;
    }

    public boolean haveAllPermissions() {
        if (mAndroidLevel < Build.VERSION_CODES.M) {
            // Pre-M, the system asked for perms before startup.
            return true;
        }
        for (String perm : mPerms) {
            if (mActivity.checkSelfPermission(perm) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    public synchronized void obtainPermission() {
        if (haveAllPermissions()) {
            return;
        }
        if (mIsRequestInProgress) {
            throw new IllegalStateException("Request in progress.");
        }
        mIsRequestInProgress = true;
        mActivity.requestPermissions(mPerms, mRequestCode);
    }

    public synchronized boolean granted(
            int requestCode, String[] permissions, int[] results) {
        mIsRequestInProgress = false;
        if (requestCode != mRequestCode) {
            return false;
        }
        if (results.length != permissions.length) {
            return false;
        }
        for (int i = 0; i < results.length; i++) {
            if (results[i] != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
}
