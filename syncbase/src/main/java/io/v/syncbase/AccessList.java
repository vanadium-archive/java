// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase;

import java.util.Map;

enum AccessLevel {
    Read,
    ReadWrite,
    ReadWriteAdmin
}

public class AccessList {
    public Map<String, AccessLevel> users;
}
