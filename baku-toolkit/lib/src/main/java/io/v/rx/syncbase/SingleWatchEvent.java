// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.rx.syncbase;

import io.v.v23.services.watch.ResumeMarker;
import io.v.v23.syncbase.nosql.ChangeType;
import io.v.v23.syncbase.nosql.WatchChange;
import io.v.v23.verror.VException;
import io.v.v23.vom.VomUtil;
import lombok.Value;
import lombok.experimental.Accessors;

@Accessors(prefix = "m")
@Value
public class SingleWatchEvent<T> {
    T mValue;
    ResumeMarker mResumeMarker;
    boolean mFromSync;

    @SuppressWarnings("unchecked")
    private static <T> T getWatchValue(final WatchChange change, final T defaultValue)
            throws VException {
        if (change.getChangeType() == ChangeType.DELETE_CHANGE) {
            return defaultValue;
        } else {
            return (T) VomUtil.decode(change.getVomValue());
        }
    }

    public static <T> SingleWatchEvent<T> fromWatchChange(final WatchChange c, final T defaultValue)
            throws VException {
        return new SingleWatchEvent<>(getWatchValue(c, defaultValue),
                c.getResumeMarker(), c.isFromSync());
    }
}
