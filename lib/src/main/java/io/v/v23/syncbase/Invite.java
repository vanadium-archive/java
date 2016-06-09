// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.syncbase;

import io.v.v23.services.syncbase.Id;

/**
 * Represents a new value for an entity watched using {@link Database#watch}.
 */
public class Invite {
    private final Id syncgroup;

    public Invite(String blessing, String name) {
        this.syncgroup = new Id(blessing, name);
    }

    public Id getSyncgroupId() {
        return syncgroup;
    }
}
