// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase;

public class Id {
    private final String mBlessing;
    private final String mName;

    // TODO(sadovsky): Eliminate the code below once we've switched to io.v.syncbase.core.

    protected Id(io.v.v23.services.syncbase.Id id) {
        mBlessing = id.getBlessing();
        mName = id.getName();
    }

    protected io.v.v23.services.syncbase.Id toVId() {
        return new io.v.v23.services.syncbase.Id(mBlessing, mName);
    }
}
