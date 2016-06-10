// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase.internal;

import io.v.syncbase.core.VError;

public class Row {
    public static native boolean Exists(String name, String batchHandle) throws VError;
    public static native byte[] Get(String name, String batchHandle) throws VError;
    public static native void Put(String name, String batchHandle, byte[] value) throws VError;
    public static native void Delete(String name, String batchHandle) throws VError;
}
