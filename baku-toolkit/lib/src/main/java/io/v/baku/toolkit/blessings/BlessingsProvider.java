// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit.blessings;

import io.v.v23.security.Blessings;
import rx.Observable;

public interface BlessingsProvider {
    /**
     * @return the connect-on-subscribe observable that provides blessings. This observable should
     * subsequently emit the same blessings as {@link #getPassiveRxBlessings()}. It might be simpler
     * to provide only a passive observable and a refresh method, but the more common use case is to
     * require up-to-date blessings on subscription.
     */
    Observable<Blessings> getRxBlessings();

    /**
     * @return the passive observable that provides blessings. This observable should emit any
     * blessings attained through active refreshes or through {@link #getRxBlessings()}.
     */
    Observable<Blessings> getPassiveRxBlessings();
}
