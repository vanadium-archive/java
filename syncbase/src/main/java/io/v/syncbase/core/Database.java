// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase.core;

import java.util.List;

public class Database extends DatabaseHandle {
    Database(Id id) {
        super(id);
    }

    public VersionedPermissions getPermissions() throws VError {
        return io.v.syncbase.internal.Database.GetPermissions(fullName);
    }

    public void setPermissions(VersionedPermissions permissions) throws VError {
        io.v.syncbase.internal.Database.SetPermissions(fullName, permissions);
    }

    public void create(Permissions permissions) throws VError {
        io.v.syncbase.internal.Database.Create(fullName, permissions);
    }

    public void destroy() throws VError {
        io.v.syncbase.internal.Database.Destroy(fullName);
    }

    public boolean exists() throws VError {
        return io.v.syncbase.internal.Database.Exists(fullName);
    }

    public BatchDatabase beginBatch(BatchOptions options) throws VError {
        String batchHandle = io.v.syncbase.internal.Database.BeginBatch(fullName, options);
        return new BatchDatabase(this.id, batchHandle);
    }

    public Syncgroup syncgroup(String name) throws VError {
        return syncgroup(new Id(io.v.syncbase.internal.Blessings.UserBlessingFromContext(), name));
    }

    public Syncgroup syncgroup(Id id) {
        return new Syncgroup(this, id);
    }

    public List<Id> listSyncgroups() throws VError {
        return io.v.syncbase.internal.Database.ListSyncgroups(fullName);
    }

    public interface WatchPatternsCallbacks {
        void onChange(WatchChange watchChange);

        void onError(VError vError);
    }

    public void watch(byte[] resumeMarker, List<CollectionRowPattern> patterns,
                      final WatchPatternsCallbacks callbacks) {
        try {
            io.v.syncbase.internal.Database.WatchPatterns(fullName, resumeMarker, patterns,
                    new io.v.syncbase.internal.Database.WatchPatternsCallbacks() {
                        @Override
                        public void onChange(WatchChange watchChange) {
                            callbacks.onChange(watchChange);
                        }

                        @Override
                        public void onError(VError vError) {
                            callbacks.onError(vError);
                        }
                    });
        } catch (VError vError) {
            callbacks.onError(vError);
        }
    }

    public interface BatchOperation {
        void run(BatchDatabase batchDatabase);
    }

    public void runInBatch(final BatchOperation op, BatchOptions options) throws VError {
        // TODO(sadovsky): Make the number of attempts configurable.
        for (int i = 0; i < 3; i++) {
            BatchDatabase batchDatabase = beginBatch(options);
            op.run(batchDatabase);
            // A readonly batch should be Aborted; Commit would fail.
            if (options.readOnly) {
                batchDatabase.abort();
                return;
            }
            try {
                batchDatabase.commit();
                return;
            } catch (VError vError) {
                // TODO(sadovsky): Commit() can fail for a number of reasons, e.g. RPC
                // failure or ErrConcurrentTransaction. Depending on the cause of failure,
                // it may be desirable to retry the Commit() and/or to call Abort().
                if (!vError.id.equals(VError.SYNCBASE_CONCURRENT_BATCH)) {
                    throw vError;
                }
            }
        }
    }
}