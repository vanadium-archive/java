// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an invitation to join a syncgroup.
 */
public class SyncgroupInvite {
    private Id mId;
    private List<String> mInviterBlessingNames;

    protected SyncgroupInvite(Id id, List<String> inviterBlessingNames) {
        mId = id;
        mInviterBlessingNames = inviterBlessingNames;
    }

    public Id getId() {
        return mId;
    }

    public User getSyncgroupCreator() {
        return new User(Syncbase.getAliasFromBlessingPattern(mId.getBlessing()));
    }

    public User getInviter() {
        if (mInviterBlessingNames.size() == 0) {
            return null;
        }
        // TODO(alexfandrianto): This will normally work because inviter blessing names should be
        // just a single name. However, this will probably not work if it's the cloud's blessing.
        return new User(Syncbase.getAliasFromBlessingPattern(mInviterBlessingNames.get(0)));
    }

    protected List<String> getInviterBlessingNames() {
        return new ArrayList<>(mInviterBlessingNames);
    }
}