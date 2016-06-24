// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase.core;

import java.util.Arrays;

import io.v.syncbase.internal.Util;
import io.v.v23.syncbase.RowRange;

public class Collection {
    private final String batchHandle;
    private final Id id;
    private final String fullName;

    Collection(String parentFullName, Id id, String batchHandle) {
        this.batchHandle = batchHandle;
        this.id = id;
        this.fullName = Util.NamingJoin(Arrays.asList(parentFullName, id.encode()));
    }

    public Id id() {
        return id;
    }

    public Permissions getPermissions() throws VError {
        return io.v.syncbase.internal.Collection.GetPermissions(fullName, batchHandle);
    }

    public void setPermissions(Permissions permissions) throws VError {
        io.v.syncbase.internal.Collection.SetPermissions(fullName, batchHandle, permissions);
    }

    public void create(Permissions permissions) throws VError {
        io.v.syncbase.internal.Collection.Create(fullName, batchHandle, permissions);
    }

    public void destroy() throws VError {}

    public boolean exists() throws VError {
        return io.v.syncbase.internal.Collection.Exists(fullName, batchHandle);
    }

    public Row row(String key) {
        return new Row(this.fullName, key, this.batchHandle);
    }

    public byte[] get(String key) throws VError {
        return new Row(this.fullName, key, this.batchHandle).get();
    }

    public void put(String key, byte[] value) throws VError {
        new Row(this.fullName, key, this.batchHandle).put(value);
    }

    public void delete(String key) throws VError {
        new Row(this.fullName, key, this.batchHandle).delete();
    }

    public void deleteRange(byte[] start, byte[] limit) throws VError {
        io.v.syncbase.internal.Collection.DeleteRange(fullName, batchHandle, start, limit);
    }

    public interface ScanCallbacks {
        void onValue(KeyValue keyValue);
        void onDone(VError vError);
    }

    public void scan(RowRange range, ScanCallbacks scanCallbacks) {}
}
