// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase.internal;

public class Init {
    public static void init() {
        System.loadLibrary("syncbase");
    }
}