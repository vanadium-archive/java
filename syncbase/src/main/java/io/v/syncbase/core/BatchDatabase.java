// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase.core;

import java.util.List;

public class BatchDatabase extends DatabaseHandle {
    private final String batchHandle;

    BatchDatabase(Id id, String batchHandle) {
        super(id);
        this.batchHandle = batchHandle;
    }

    @Override
    public Collection collection(Id id) {
        return new Collection(this.fullName, id, this.batchHandle);
    }

    @Override
    public List<Id> listCollections() throws VError {
        return io.v.syncbase.internal.Database.ListCollections(fullName, batchHandle);
    }

    @Override
    public byte[] getResumeMarker() throws VError {
        return io.v.syncbase.internal.Database.GetResumeMarker(fullName, batchHandle);
    }

    public void commit() throws VError {
        io.v.syncbase.internal.Database.Commit(fullName, batchHandle);
    }

    public void abort() throws VError {
        io.v.syncbase.internal.Database.Abort(fullName, batchHandle);
    }
}
