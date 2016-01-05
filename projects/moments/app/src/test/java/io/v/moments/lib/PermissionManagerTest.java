// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.moments.lib;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

@RunWith(MockitoJUnitRunner.class)
public class PermissionManagerTest {
    static final int REQUEST_CODE = 919;
    static final String PERM0 = "perm0";
    static final String PERM1 = "perm1";
    static final String[] NO_PERMS = {};
    static final String[] REQUIRED_PERMS = {PERM0, PERM1};

    @Rule
    public ExpectedException mThrown = ExpectedException.none();

    @Mock
    Activity mActivity;

    PermissionManager mManager;

    @Before
    public void setup() {
        mManager = new PermissionManager(
                Build.VERSION_CODES.M, mActivity, REQUEST_CODE, REQUIRED_PERMS);
    }

    @Test
    public void simpleHaveAllPermissions() {
        mManager = new PermissionManager(
                Build.VERSION_CODES.M, mActivity, REQUEST_CODE, NO_PERMS);
        assertTrue(mManager.haveAllPermissions());
    }

    @Test
    public void preMarshmallowPermissions() {
        mManager = new PermissionManager(
                Build.VERSION_CODES.LOLLIPOP, mActivity, REQUEST_CODE, REQUIRED_PERMS);
        assertTrue(mManager.haveAllPermissions());
    }

    @Test
    public void haveAllPermissionsYes() {
        when(mActivity.checkSelfPermission(PERM0)).thenReturn(PackageManager.PERMISSION_GRANTED);
        when(mActivity.checkSelfPermission(PERM1)).thenReturn(PackageManager.PERMISSION_GRANTED);
        assertTrue(mManager.haveAllPermissions());
    }

    @Test
    public void haveAllPermissionsNo() {
        when(mActivity.checkSelfPermission(PERM0)).thenReturn(PackageManager.PERMISSION_GRANTED);
        when(mActivity.checkSelfPermission(PERM1)).thenReturn(PackageManager.PERMISSION_DENIED);
        assertFalse(mManager.haveAllPermissions());
    }

    @Test
    public void obtainPermission() {
        when(mActivity.checkSelfPermission(PERM0)).thenReturn(PackageManager.PERMISSION_GRANTED);
        when(mActivity.checkSelfPermission(PERM1)).thenReturn(PackageManager.PERMISSION_DENIED);
        mManager.obtainPermission();
        verify(mActivity).requestPermissions(REQUIRED_PERMS, REQUEST_CODE);
    }

    @Test
    public void grantedNotEnough() {
        int[] notEnough = {PackageManager.PERMISSION_GRANTED};
        assertFalse(mManager.granted(REQUEST_CODE, REQUIRED_PERMS, notEnough));
    }

    @Test
    public void grantedDenied() {
        int[] denied = {PackageManager.PERMISSION_GRANTED, PackageManager.PERMISSION_DENIED};
        assertFalse(mManager.granted(REQUEST_CODE, REQUIRED_PERMS, denied));
    }

    @Test
    public void grantedWrongCode() {
        int[] granted = {PackageManager.PERMISSION_GRANTED, PackageManager.PERMISSION_GRANTED};
        assertFalse(mManager.granted(REQUEST_CODE + 1, REQUIRED_PERMS, granted));
    }

    @Test
    public void grantedOkay() {
        int[] granted = {PackageManager.PERMISSION_GRANTED, PackageManager.PERMISSION_GRANTED};
        assertTrue(mManager.granted(REQUEST_CODE, REQUIRED_PERMS, granted));
    }
}
