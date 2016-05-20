// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase;

import java.util.Map;

public class AccessList {
    public enum AccessLevel {
        READ,
        READ_WRITE,
        READ_WRITE_ADMIN
    }

    public Map<String, AccessLevel> users;
}
