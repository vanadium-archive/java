// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase.internal;

public class Blessings {
    public static native String DebugString();
    public static native String AppBlessingFromContext() throws VError;
    public static native String UserBlessingFromContext() throws VError;
}
