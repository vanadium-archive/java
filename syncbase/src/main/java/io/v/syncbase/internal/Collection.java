// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase.internal;

import io.v.syncbase.core.KeyValue;
import io.v.syncbase.core.Permissions;
import io.v.syncbase.core.VError;

public class Collection {
    public static native Permissions GetPermissions(String name, String batchHandle) throws VError;
    public static native void SetPermissions(String name, String batchHandle, Permissions permissions) throws VError;

    public static native void Create(String name, String batchHandle, Permissions permissions) throws VError;
    public static native void Destroy(String name, String batchHandle) throws VError;
    public static native boolean Exists(String name, String batchHandle) throws VError;
    public static native void DeleteRange(String name, String batchHandle, byte[] start, byte[] limit) throws VError;

    public interface ScanCallbacks {
        void onKeyValue(KeyValue keyValue);
        void onDone(VError vError);
    }

    public static native void Scan(String name, String batchHandle, byte[] start, byte[] limit, ScanCallbacks callbacks) throws VError;
}