// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.rx.syncbase;

import com.google.common.collect.ImmutableMap;

import org.apache.commons.io.FileUtils;

import java.io.File;

import io.v.android.v23.V;
import io.v.baku.toolkit.VAndroidTestCase;
import io.v.baku.toolkit.blessings.BlessingsUtils;
import io.v.impl.google.services.mounttable.MountTableServer;
import io.v.v23.context.VContext;
import io.v.v23.rpc.ListenSpec;
import io.v.v23.rpc.Server;
import lombok.experimental.Delegate;

public class SgHostUtilLocalTest extends VAndroidTestCase {
    private File mStorageRoot;
    private Server mMountTable;

    @Delegate
    private SgHostUtilTestCases mTestCases;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        mStorageRoot = new File(getContext().getFilesDir(), "mounttable");
        mStorageRoot.mkdirs();

        VContext nsctx = getVContext();
        nsctx = V.withListenSpec(nsctx, V.getListenSpec(nsctx).withAddress(
                new ListenSpec.Address("tcp", "localhost:0")));
        nsctx = MountTableServer.withNewServer(nsctx, new MountTableServer.Params()
                .withStorageRootDir(mStorageRoot.getAbsolutePath())
                .withPermissions(ImmutableMap.of("test", BlessingsUtils.OPEN_DATA_PERMS))
                .withStatsPrefix("test"));

        // TODO(rosswang): We might like to do this by changing the namespace root, but
        // https://github.com/vanadium/issues/issues/1052

        mMountTable = V.getServer(nsctx);
        final String endpoint = mMountTable.getStatus().getEndpoints()[0].name();
        mTestCases = new SgHostUtilTestCases(getContext(), getVContext(), endpoint);
    }

    @Override
    protected void tearDown() throws Exception {
        mMountTable.stop();
        FileUtils.deleteDirectory(mStorageRoot);
        super.tearDown();
    }
}
