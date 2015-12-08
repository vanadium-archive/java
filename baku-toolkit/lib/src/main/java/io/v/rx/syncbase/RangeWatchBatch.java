// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.rx.syncbase;

import java.util.ArrayList;
import java.util.List;

import io.v.v23.services.watch.ResumeMarker;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;
import rx.Observable;

@Accessors(prefix = "m")
@AllArgsConstructor
@Getter
public class RangeWatchBatch<T> {
    private final ResumeMarker mResumeMarker;
    private final Observable<RangeWatchEvent<T>> mChanges;

    public Observable<List<RangeWatchEvent<T>>> collectChanges() {
        return mChanges.collect(ArrayList::new, List::add);
    }
}
