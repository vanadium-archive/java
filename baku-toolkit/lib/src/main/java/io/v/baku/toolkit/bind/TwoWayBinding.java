// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit.bind;

import io.v.rx.syncbase.SingleWatchEvent;
import rx.Observable;
import rx.Subscription;

/**
 * This interface represents a bidirectional data binding between a Syncbase row and an Android
 * widget. Bindings are nested from the Syncbase layer innermost to the UI layer outermost. The
 * read link is the data flow from Syncbase to the UI, and the write link is the data flow from the
 * UI to Syncbase.
 */
public interface TwoWayBinding<T> {
    /**
     * Links the data flow from Syncbase to the UI. This method should be called at most once per
     * instance, and the observable should have at most one subscriber.
     */
    Observable<SingleWatchEvent<T>> linkRead();

    /**
     * Links the data flow from the UI to Syncbase. This method should be called at most once per
     * instance, and the observable should have at most one subscriber.
     */
    Subscription linkWrite(Observable<T> rxData);
}
