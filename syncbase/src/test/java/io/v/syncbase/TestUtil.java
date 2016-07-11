// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase;

import com.google.common.util.concurrent.SettableFuture;

import java.io.File;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

import io.v.syncbase.exception.SyncbaseException;

class TestUtil {
    private static final Executor sameThreadExecutor = new Executor() {
        public void execute(Runnable runnable) {
            runnable.run();
        }
    };

    static Database createDatabase() throws ExecutionException, InterruptedException,
            SyncbaseException {
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

        future.get();
        return Syncbase.database();
    }

    static void setUpSyncbase(File folder) throws SyncbaseException, ExecutionException,
            InterruptedException {
        Syncbase.Options opts = new Syncbase.Options();
        opts.rootDir = folder.getAbsolutePath();
        opts.disableUserdataSyncgroup = true;
        opts.disableSyncgroupPublishing = true;
        opts.testLogin = true;
        opts.callbackExecutor = sameThreadExecutor;
        Syncbase.init(opts);
    }

    static void setUpDatabase(File folder) throws SyncbaseException, ExecutionException,
            InterruptedException {
        setUpSyncbase(folder);
        createDatabase();
    }
}
