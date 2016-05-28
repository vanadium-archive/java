// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase;

/**
 * Uniquely identifies a database, collection, or syncgroup.
 */
public class Id {
    private final String mBlessing;
    private final String mName;

    public Id(String blessing, String name) {
        mBlessing = blessing;
        mName = name;
    }

    // TODO(sadovsky): Add encode and decode methods.

    public String getBlessing() {
        return mBlessing;
    }

    public String getName() {
        return mName;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Id && other != null) {
            Id otherId = (Id)other;
            return mBlessing.equals(otherId.getBlessing()) && mName.equals(otherId.getName());
        }
        return false;
    }

    @Override
    public int hashCode() {
        // Note: Copied from VDL.
        int result = 1;
        int prime = 31;

        result = prime * result + (mBlessing == null ? 0 : mBlessing.hashCode());

        result = prime * result + (mName == null ? 0 : mName.hashCode());

        return result;
    }

    // TODO(sadovsky): Eliminate the code below once we've switched to io.v.syncbase.core.

    protected Id(io.v.v23.services.syncbase.Id id) {
        mBlessing = id.getBlessing();
        mName = id.getName();
    }

    protected io.v.v23.services.syncbase.Id toVId() {
        return new io.v.v23.services.syncbase.Id(mBlessing, mName);
    }
}
