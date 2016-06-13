// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase.core;

import java.util.List;

public class DatabaseHandle {
    protected Id id;
    protected String fullName;

    protected DatabaseHandle(Id id) {
        this.id = id;
        fullName = id.encode();
    }

    public Id id() {
        return id;
    }

    public Collection collection(String name) throws VError {
        return collection(new Id(io.v.syncbase.internal.Blessings.UserBlessingFromContext(), name));
    }

    public Collection collection(Id id) {
        return new Collection(new BatchDatabase(this.id, "").fullName, id, "");
    }

    public List<Id> listCollections() throws VError {
        return io.v.syncbase.internal.Database.ListCollections(fullName, "");
    }

    public byte[] getResumeMarker() throws VError {
        return io.v.syncbase.internal.Database.GetResumeMarker(fullName, "");
    }
}
