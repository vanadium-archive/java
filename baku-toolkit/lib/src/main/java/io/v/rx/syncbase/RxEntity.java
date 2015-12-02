// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.rx.syncbase;

import rx.Observable;

public abstract class RxEntity<T, P> {
    public abstract String getName();
    public abstract Observable<T> getObservable();
    public abstract Observable<T> mapFrom(P parent);

    /**
     * This is a shortcut for {@code getObservable().first()}, to reduce the likelihood of
     * forgetting to filter by {@code first}. Most commands should be run in this way.
     */
    public Observable<T> once() {
        return getObservable().first();
    }
}
