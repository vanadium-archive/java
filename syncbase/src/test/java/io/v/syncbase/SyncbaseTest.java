// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase;

import com.google.common.util.concurrent.SettableFuture;

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import io.v.v23.V;
import io.v.v23.context.VContext;
import io.v.v23.rpc.ListenSpec;

public class SyncbaseTest {
    private VContext ctx;

    // To run these tests from Android Studio, add the following VM option to the default JUnit
    // build configuration, via Run > Edit Configurations... > Defaults > JUnit > VM options:
    // -Djava.library.path=/Users/sadovsky/vanadium/release/java/syncbase/build/libs
    @Before
    public void setUp() throws Exception {
        ctx = V.init();
        ctx = V.withListenSpec(ctx, V.getListenSpec(ctx).withAddress(
                new ListenSpec.Address("tcp", "localhost:0")));
    }

    @Test
    public void createDatabase() throws Exception {
        Syncbase.DatabaseOptions opts = new Syncbase.DatabaseOptions();
        opts.rootDir = "/tmp";
        opts.disableUserdataSyncgroup = true;
        opts.vContext = ctx;

        final SettableFuture<Database> future = SettableFuture.create();

        Syncbase.database(new Syncbase.DatabaseCallback() {
            @Override
            public void onSuccess(Database db) {
                future.set(db);
            }

            @Override
            public void onError(Throwable e) {
                future.setException(e);
            }
        }, opts);

        future.get(5, TimeUnit.SECONDS);
    }
}