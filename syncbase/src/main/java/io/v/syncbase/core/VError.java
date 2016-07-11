// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase.core;

public class VError extends Exception {
    public static final String EXIST = "v.io/v23/verror.Exist";
    public static final String NO_EXIST = "v.io/v23/verror.NoExist";
    public static final String SYNCBASE_CONCURRENT_BATCH = "v.io/v23/services/syncbase.ConcurrentBatch";

    public String id;
    public long actionCode;
    public String message;
    public String stack;

    /** Called in JNI */
    private VError() {
    }

    public String toString() {
        return String.format("{\n  id: \"%s\"\n  actionCode: %d\n  message: \"%s\"\n  stack: \"%s\"}",
                id, actionCode, message, stack);
    }
}