// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase.core;

import java.util.List;

public class SyncgroupInvite {
    public Id syncgroup;
    public List<String> addresses;
    public List<String> blessingNames;
}
