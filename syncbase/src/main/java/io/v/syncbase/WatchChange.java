// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase;

import com.google.common.collect.Lists;

import java.util.List;

public class WatchChange {
    public enum ChangeType {
        PUT,
        DELETE;
    }

    private final ChangeType mChangeType;
    private final Id mCollectionId;
    private final String mRowKey;
    private final Object mValue;
    private final byte[] mResumeMarker;
    private final boolean mFromSync;
    private final boolean mContinued;

    // TODO(sadovsky): Eliminate the code below once we've switched to io.v.syncbase.core.

    protected WatchChange(io.v.v23.syncbase.WatchChange c) {
        mChangeType = c.getChangeType() == io.v.v23.syncbase.ChangeType.PUT_CHANGE ? ChangeType.PUT : ChangeType.DELETE;
        mCollectionId = new Id(c.getCollectionId());
        mRowKey = c.getRowName();
        mValue = c.getValue();
        List<Byte> bytes = Lists.newArrayList(c.getResumeMarker().iterator());
        mResumeMarker = new byte[bytes.size()];
        for (int i = 0; i < mResumeMarker.length; i++) mResumeMarker[i] = (byte) bytes.get(i);
        mFromSync = c.isFromSync();
        mContinued = c.isContinued();
    }

    public ChangeType getChangeType() {
        return mChangeType;
    }

    public Id getCollectionId() {
        return mCollectionId;
    }

    public String getRowKey() {
        return mRowKey;
    }

    public Object getValue() {
        return mValue;
    }

    public byte[] getResumeMarker() {
        return mResumeMarker;
    }

    public boolean isFromSync() {
        return mFromSync;
    }

    public boolean isContinued() {
        return mContinued;
    }
}