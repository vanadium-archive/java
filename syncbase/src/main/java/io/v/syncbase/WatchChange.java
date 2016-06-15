// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase;

/**
 * Describes a change to a database.
 */
public class WatchChange {
    public enum ChangeType {
        PUT,
        DELETE
    }

    private final ChangeType mChangeType;
    private final Id mCollectionId;
    private final String mRowKey;
    private final Object mValue;
    private final byte[] mResumeMarker;
    private final boolean mFromSync;
    private final boolean mContinued;

    protected WatchChange(io.v.syncbase.core.WatchChange change) {
        mChangeType = change.changeType == io.v.syncbase.core.WatchChange.ChangeType.PUT ?
                ChangeType.PUT : ChangeType.DELETE;
        mCollectionId = new Id(change.collection);
        mRowKey = change.row;
        mValue = change.value;
        mResumeMarker = change.resumeMarker.getBytes();
        mFromSync = change.fromSync;
        mContinued = change.continued;
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