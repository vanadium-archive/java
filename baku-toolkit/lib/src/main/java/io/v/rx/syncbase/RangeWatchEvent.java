// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.rx.syncbase;

import com.google.common.reflect.TypeToken;

import java.util.Map;

import io.v.v23.syncbase.nosql.ChangeType;
import io.v.v23.syncbase.nosql.WatchChange;
import io.v.v23.verror.VException;
import io.v.v23.vom.VomUtil;
import lombok.Value;
import lombok.experimental.Accessors;

@Accessors(prefix = "m")
@Value
public class RangeWatchEvent<T> {
    RxTable.Row<T> mRow;
    ChangeType mChangeType;
    boolean mFromSync;

    @SuppressWarnings("unchecked")
    private static <T> T getWatchValue(final WatchChange change, final TypeToken<T> tt)
            throws VException {
        if (change.getChangeType() == ChangeType.DELETE_CHANGE) {
            return null;
        } else {
            return (T) VomUtil.decode(change.getVomValue(),
                    tt == null? Object.class : tt.getType());
        }
    }

    public static <T> RangeWatchEvent<T> fromWatchChange(final WatchChange c, final TypeToken<T> tt)
            throws VException {
        return new RangeWatchEvent<>(new RxTable.Row<>(c.getRowName(), getWatchValue(c, tt)),
                c.getChangeType(), c.isFromSync());
    }

    public static <T> RangeWatchEvent<T> fromWatchChange(final WatchChange c, final Class<T> type)
            throws VException {
        return fromWatchChange(c, TypeToken.of(type));
    }

    public void applyTo(final Map<String, T> accumulator) {
        if (mChangeType == ChangeType.DELETE_CHANGE) {
            accumulator.remove(mRow.getRowName());
        } else {
            accumulator.put(mRow.getRowName(), mRow.getValue());
        }
    }
}
