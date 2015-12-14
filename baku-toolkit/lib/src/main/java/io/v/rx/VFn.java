// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.rx;

import io.v.v23.verror.VException;
import java8.lang.FunctionalInterface;
import lombok.experimental.UtilityClass;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;

@UtilityClass
public class VFn {
    @FunctionalInterface
    public interface VAction1<T> {
        void call(T arg) throws VException;
    }

    @FunctionalInterface
    public interface VFunc1<T, R> {
        R call(T arg) throws VException;
    }

    public static <T> Action1<T> unchecked(final VAction1<? super T> v) {
        return t -> {
            try {
                v.call(t);
            } catch (final VException e) {
                throw new UncheckedVException(e);
            }
        };
    }

    public static <T, R> Func1<T, Observable<R>> wrap(final VFunc1<? super T, ? extends R> v) {
        return t -> {
            try {
                return Observable.just(v.call(t));
            } catch (final VException e) {
                return Observable.error(e);
            }
        };
    }
}
