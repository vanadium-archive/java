// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.moments.v23.impl;

import android.app.Activity;
import android.content.Context;

import com.google.common.util.concurrent.FutureCallback;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import io.v.v23.security.Blessings;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class V23ManagerImplTest {

    @Rule
    public ExpectedException mThrown = ExpectedException.none();

    @Mock
    Activity mActivity;
    @Mock
    Context mContext;

    V23ManagerImpl mManager;

    FutureCallback<Blessings> makeBlessingsCallback() {
        return new FutureCallback<Blessings>() {
            @Override
            public void onSuccess(Blessings blessings) {
            }

            @Override
            public void onFailure(final Throwable t) {
            }
        };
    }

    @Before
    public void setup() throws Exception {
        mManager = new V23ManagerImpl();
        when(mActivity.getApplicationContext()).thenReturn(mContext);
    }

    // Disabled @Test
    public void initialization() throws Exception {
        mManager.init(mActivity, makeBlessingsCallback());
        mManager.shutdown();
    }

    @Test
    public void placeholder() {
        // TODO(jregan): Add tests.  Tricky because must start v23 runtime.
    }
}
