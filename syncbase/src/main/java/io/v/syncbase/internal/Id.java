// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase.internal;

public class Id {
    String blessing;
    String name;

    public Id() {
        // This empty constructor makes the JNI code a little bit simpler by making this class
        // similar to other classes for which we cache class/method/field IDs.
    }

    public Id(String blessing, String name) {
        this.blessing = blessing;
        this.name = name;
    }

    // TODO(razvanm): Switch to v23_syncbase_EncodeId.
    // Reference: https://github.com/vanadium/go.v23/blob/master/syncbase/util/util.go
    public String toString() {
        return blessing + "," + name;
    }
}
