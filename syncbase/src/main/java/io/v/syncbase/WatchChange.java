// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase;

import io.v.syncbase.exception.SyncbaseException;
import io.v.syncbase.exception.SyncbaseInternalException;
import io.v.v23.verror.VException;
import io.v.v23.vom.VomUtil;

import static io.v.syncbase.exception.Exceptions.chainThrow;

/**
 * Describes a change to a database.
 */
public class WatchChange {
    public enum EntityType {
        COLLECTION,
        ROW
    }

    public enum ChangeType {
        PUT,
        DELETE
    }

    private final EntityType mEntityType;
    private final ChangeType mChangeType;
    private final Id mCollectionId;
    private final String mRowKey;
    private final byte[] mValue;
    private final byte[] mResumeMarker;
    private final boolean mFromSync;
    private final boolean mContinued;

    WatchChange(io.v.syncbase.core.WatchChange change) {
        // TODO(eobrain): use switch statements below
        if (change.entityType == io.v.syncbase.core.WatchChange.EntityType.COLLECTION) {
            mEntityType = EntityType.COLLECTION;
        } else if (change.entityType == io.v.syncbase.core.WatchChange.EntityType.ROW) {
            mEntityType = EntityType.ROW;
        } else {
            throw new SyncbaseInternalException("Unknown EntityType: " + change.entityType);
        }
        if (change.changeType == io.v.syncbase.core.WatchChange.ChangeType.PUT) {
            mChangeType = ChangeType.PUT;
        } else if (change.changeType == io.v.syncbase.core.WatchChange.ChangeType.DELETE) {
            mChangeType = ChangeType.DELETE;
        } else {
            throw new SyncbaseInternalException("Unknown ChangeType: " + change.changeType);
        }
        mCollectionId = new Id(change.collection);
        mRowKey = change.row;
        mValue = change.value;
        mResumeMarker = change.resumeMarker;
        mFromSync = change.fromSync;
        mContinued = change.continued;
    }

    public EntityType getEntityType() {
        return mEntityType;
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

    public <T> T getValue(Class<T> cls) throws SyncbaseException {
        try {
            return (T) VomUtil.decode(mValue, cls);
        } catch (VException e) {
            chainThrow("getting value from a WatchChange of collection",  mCollectionId, e);
            throw new AssertionError("never happens");
        }
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

    public String toString() {
        return String.format("{\n  entityType: \"%s\"\n  changeType: \"%s\"\n" +
                "  collectionId: %s\n  rowKey: \"%s\"\n  fromSync: %s\n" +
                "  continued: %s\n}",
                mEntityType, mChangeType, mCollectionId, mRowKey, mFromSync, mContinued);
    }
}