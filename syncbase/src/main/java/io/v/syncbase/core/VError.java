// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase.core;

public class VError extends Exception {
    public String id;
    public long actionCode;
    public String message;
    public String stack;

    public String toString() {
        return String.format("{\n  id: \"%s\"\n  actionCode: %d\n  message: \"%s\"\n  stack: \"%s\"}",
                id, actionCode, message, stack);
    }
}