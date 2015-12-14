// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit.bind;

import io.v.rx.syncbase.SingleWatchEvent;
import rx.Observable;
import rx.Subscription;

/**
 * {@code TwoWayBinding}s are nested from the Syncbase layer innermost to the UI layer outermost.
 * The downlink is the data flow from Syncbase to the UI, and the uplink is the data flow from the
 * UI to Syncbase.
 */
public interface TwoWayBinding<T> {
    /**
     * This method should be called at most once per instance, and the observable should have at
     * most one subscriber.
     */
    Observable<SingleWatchEvent<T>> downlink();

    /**
     * This method should be called at most once per instance, and the observable should have at
     * most one subscriber.
     */
    Subscription uplink(Observable<T> rxData);
}
