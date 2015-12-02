// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.rx;

import io.v.v23.verror.VException;
import java8.lang.FunctionalInterface;
import lombok.experimental.UtilityClass;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;

@UtilityClass
public class VFn {
    @FunctionalInterface
    public interface VAction {
        void call() throws VException;
    }

    @FunctionalInterface
    public interface VAction1<T> {
        void call(T arg) throws VException;
    }

    @FunctionalInterface
    public interface VFunc1<T, R> {
        R call(T arg) throws VException;
    }

    public static void doUnchecked(final VAction v) {
        try {
            v.call();
        } catch (final VException e) {
            throw new UncheckedVException(e);
        }
    }

    public static Action0 unchecked(final VAction v) {
        return () -> doUnchecked(v);
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

    public static <T, R> Func1<T, R> unchecked(final VFunc1<? super T, ? extends R> v) {
        return t -> {
            try {
                return v.call(t);
            } catch (final VException e) {
                throw new UncheckedVException(e);
            }
        };
    }
}
