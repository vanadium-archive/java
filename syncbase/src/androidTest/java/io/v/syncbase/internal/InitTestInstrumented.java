// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase.internal;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.SmallTest;

import com.google.common.util.concurrent.SettableFuture;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeUnit;

import io.v.syncbase.Syncbase;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class InitTestInstrumented {
    @Test
    public void init() throws Exception {
        Syncbase.Options opts = new Syncbase.Options();
        opts.rootDir = InstrumentationRegistry.getContext()
                .getDir("syncbase", Context.MODE_PRIVATE).getAbsolutePath();
        opts.disableUserdataSyncgroup = true;
        opts.disableSyncgroupPublishing = true;
        opts.testLogin = true;
        Syncbase.init(opts);

        final SettableFuture<Void> future = SettableFuture.create();

        Syncbase.login("", "", new Syncbase.LoginCallback() {
            @Override
            public void onSuccess() {
                future.set(null);
            }

            @Override
            public void onError(Throwable e) {
                future.setException(e);
            }
        });

        future.get(5, TimeUnit.SECONDS);
    }
}
