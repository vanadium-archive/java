// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbench;

import com.google.caliper.AfterExperiment;
import com.google.caliper.BeforeExperiment;
import com.google.caliper.Benchmark;
import com.google.caliper.runner.CaliperMain;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteStreams;
import com.google.common.io.Resources;
import io.v.impl.google.services.syncbase.SyncbaseServer;
import io.v.v23.InputChannels;
import io.v.v23.V;
import io.v.v23.context.VContext;
import io.v.v23.rpc.Server;
import io.v.v23.security.BlessingPattern;
import io.v.v23.security.access.AccessList;
import io.v.v23.security.access.Constants;
import io.v.v23.security.access.Permissions;
import io.v.v23.syncbase.Syncbase;
import io.v.v23.syncbase.SyncbaseApp;
import io.v.v23.syncbase.SyncbaseService;
import io.v.v23.syncbase.nosql.Database;
import io.v.v23.syncbase.nosql.DatabaseCore;
import io.v.v23.syncbase.nosql.Table;
import io.v.v23.vdl.VdlAny;
import io.v.v23.verror.VException;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static io.v.v23.VFutures.sync;

/**
 * To run these benchmarks:
 *
 * <ul>
 *     <li>cd to the project root directory
 *     <li>./gradlew installDist
 *     <li>build/install/syncbench/bin/syncbench -i runtime
 * </ul>
 */
public class SyncbaseBenchmark {

    Server syncbaseServer;
    byte[] imageBytes;
    VContext baseContext;
    Database database;

    @BeforeExperiment
    public void setUp() throws IOException, SyncbaseServer.StartException, VException {
        imageBytes = ByteStreams.toByteArray(Resources.getResource("car.png").openStream());
        baseContext = V.init();

        AccessList acl = new AccessList(
                ImmutableList.of(new BlessingPattern("...")), ImmutableList.<String>of());
        Permissions permissions = new Permissions(ImmutableMap.of(
                Constants.READ.getValue(), acl,
                Constants.WRITE.getValue(), acl,
                Constants.ADMIN.getValue(), acl));

        baseContext = SyncbaseServer.withNewServer(baseContext, new SyncbaseServer.Params()
                .withPermissions(permissions).withStorageRootDir("/tmp/foo-" + System
                        .currentTimeMillis()));
        syncbaseServer = V.getServer(baseContext);
        SyncbaseService service = Syncbase.newService("/" + syncbaseServer.getStatus().getEndpoints()[0]);
        SyncbaseApp app = service.getApp("someApp");
        sync(app.create(baseContext, null));
        database = app.getNoSqlDatabase("foo", null);
        sync(database.create(baseContext, null));
        sync(database.getTable("someTable").create(baseContext, null));
        sync(database.getTable("someTable").put(baseContext, "testKey", imageBytes, byte[].class));
    }

    @AfterExperiment
    public void tearDown() throws VException {
        syncbaseServer.stop();
    }

    @Benchmark
    public void benchmarkImageFetchingByLookup(int reps) throws VException {
        Table table = database.getTable("someTable");
        for (int i = 0; i < reps; i++) {
            byte[] fetchedBytes = (byte[]) sync(table.getRow("testKey").get(baseContext, byte[].class));
            if (!Arrays.equals(fetchedBytes, imageBytes)) {
                throw new IllegalStateException("fetched bytes do not match");
            }
        }
    }

    @Benchmark
    public void benchmarkImageFetchingByQuery(int reps) throws VException {
        for (int i = 0; i < reps; i++) {
            DatabaseCore.QueryResults stream = sync(database.exec(baseContext, "select v from someTable"));
            for (List<VdlAny> result : InputChannels.asIterable(stream)) {
                byte[] fetchedBytes = (byte[]) result.get(0).getElem();
                if (!Arrays.equals(fetchedBytes, imageBytes)) {
                    throw new IllegalStateException("fetched bytes do not match");
                }
            }
        }
    }

    public static void main(String[] args) {
        CaliperMain.main(SyncbaseBenchmark.class, args);
    }
}
