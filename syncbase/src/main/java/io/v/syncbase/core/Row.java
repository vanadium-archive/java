// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase.core;

import java.util.Arrays;

import io.v.syncbase.internal.Util;

public class Row {
    private String batchHandle;
    private String key;
    private String fullName;

    protected Row(String parentFullName, String key, String batchHandle) {
        this.batchHandle = batchHandle;
        this.key = key;
        this.fullName = Util.NamingJoin(Arrays.asList(parentFullName, key));
    }

    public String key() {
        return key;
    }

    public boolean exists() throws VError {
        return io.v.syncbase.internal.Row.Exists(fullName, batchHandle);
    }

    public byte[] get() throws VError {
        return io.v.syncbase.internal.Row.Get(fullName, batchHandle);
    }

    public void put(byte[] value) throws VError {
        io.v.syncbase.internal.Row.Put(fullName, batchHandle, value);
    }

    public void delete() throws VError {
        io.v.syncbase.internal.Row.Delete(fullName, batchHandle);
    }
}
