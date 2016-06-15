// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase.core;

public class WatchChange {
    public enum ChangeType { PUT, DELETE }

    public Id collection;
    public String row;
    public ChangeType changeType;
    public byte[] value;
    // TODO(razvanm): Switch to byte[].
    public String resumeMarker;
    public boolean fromSync;
    public boolean continued;
}