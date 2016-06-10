// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase.core;

import java.util.List;

public class SyncgroupSpec {
    public String description;
    public String publishSyncbaseName;
    public Permissions permissions;
    public List<Id> collections;
    public List<String> mountTables;
    public boolean isPrivate;
}