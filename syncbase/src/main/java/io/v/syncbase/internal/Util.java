// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase.internal;

import java.util.List;

public class Util {
    public static native String Encode(String s);
    public static native String EncodeId(Id id);
    public static native String NamingJoin(List<String> elements);
}
