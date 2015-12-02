// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.rx;

import io.v.v23.VIterable;
import io.v.v23.verror.VException;
import lombok.experimental.UtilityClass;
import rx.Observable;

@UtilityClass
public class RxVIterable {
    /**
     * Wraps a {@link VIterable} in an observable that produces the same elements and checks for
     * an error from the {@code VIterable} at the end if present. This is a thin wrapper that does
     * not ensure that the {@code VIterable} is only iterated over once, so the returned
     * {@link Observable} should be subscribed to at most once. If multiple subscriptions are
     * needed, consider a connectable observable operator, such as {@link Observable#publish()},
     * {@link Observable#replay()}, {@link Observable#share()}, or {@link Observable#cache()}.
     * However, if using a replay/cache, be cognizant of buffer growth.
     *
     * @return an observable wrapping the {@link VIterable}. This observable should only be
     * subscribed to once, as we can only iterate over the underlying stream once.
     */
    public static <T> Observable<T> wrap(final VIterable<T> vi) {
        return Observable.from(vi)
                .concatWith(Observable.defer(() -> {
                    final VException e = vi.error();
                    return e == null ? Observable.empty() : Observable.error(e);
                }));
    }
}
