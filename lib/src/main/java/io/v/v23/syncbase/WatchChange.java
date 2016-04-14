// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.syncbase;

import io.v.v23.services.syncbase.Id;
import io.v.v23.services.watch.ResumeMarker;
import io.v.v23.vdl.VdlAny;

/**
 * Represents a new value for an entity watched using {@link Database#watch}.
 */
public class WatchChange {
    private final Id collectionId;
    private final String rowName;
    private final ChangeType changeType;
    private final VdlAny value;
    private final ResumeMarker resumeMarker;
    private final boolean fromSync;
    private final boolean continued;

    public WatchChange(Id collectionId, String rowName, ChangeType changeType, VdlAny value,
                       ResumeMarker resumeMarker, boolean fromSync, boolean continued) {
        this.collectionId = collectionId;
        this.rowName = rowName;
        this.changeType = changeType;
        this.value = value;
        this.resumeMarker = resumeMarker;
        this.fromSync = fromSync;
        this.continued = continued;
    }

    /**
     * Returns the id of the collection that contains the changed row.
     */
    public Id getCollectionId() {
        return collectionId;
    }

    /**
     * Returns the name (i.e., key) of the changed row.
     */
    public String getRowName() {
        return rowName;
    }

    /**
     * Returns the type of the change.
     * <p>
     * If {@link ChangeType#PUT_CHANGE}, the row exists in the collection and {@link #getValue()}
     * will return the new value for the row.
     * <p>
     * If {@link ChangeType#DELETE_CHANGE}, the row was removed from the collection and
     * {@link #getValue()} will return {@code null}.
     */
    public ChangeType getChangeType() {
        return changeType;
    }

    /**
     * Returns the new value for the row.
     * Requires: the {@link ChangeType} is {@link ChangeType#PUT_CHANGE}.
     */
    public Object getValue() {
        return value.getElem();
    }

    /**
     * Returns a {@link ResumeMarker} that can be used to resume the change stream from the point
     * right after this change.
     */
    public ResumeMarker getResumeMarker() {
        return resumeMarker;
    }

    /**
     * Indicates whether the change came from sync or from the local device.
     */
    public boolean isFromSync() {
        return fromSync;
    }

    /**
     * If {@code true}, this change is followed by more changes that are in the same batch
     * as this change.
     */
    public boolean isContinued() {
        return continued;
    }
}
