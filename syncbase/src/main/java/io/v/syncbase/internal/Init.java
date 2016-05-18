// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase.internal;

public class Init {
    public static void init() {
        // TODO(razvanm): Call System.loadLibrary("syncbase") to load the .so
        // that implements the expected functions. This will also call the
        // JNI_OnLoad (if we choose to use it).
    }
}