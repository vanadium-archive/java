// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase;

import java.util.List;

/**
 * Represents an invitation to join a syncgroup.
 */
public class SyncgroupInvite {
    private Id mId;
    private String mRemoteSyncbaseName;
    private List<String> mExpectedSyncbaseBlessings;

    protected SyncgroupInvite(Id id, String remoteSyncbaseName, List<String> expectedSyncbaseBlessings) {
        mId = id;
        mRemoteSyncbaseName = remoteSyncbaseName;
        mExpectedSyncbaseBlessings = expectedSyncbaseBlessings;
    }

    public Id getId() {
        return mId;
    }

    public String getRemoteSyncbaseName() {
        return mRemoteSyncbaseName;
    }

    public List<String> getExpectedSyncbaseBlessings() {
        return mExpectedSyncbaseBlessings;
    }
}