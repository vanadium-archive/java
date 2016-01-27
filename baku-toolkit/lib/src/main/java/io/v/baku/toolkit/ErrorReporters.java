// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit;

import android.app.Fragment;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import rx.functions.Action1;

@Slf4j
@UtilityClass
public class ErrorReporters {
    public static ErrorReporter forFragment(final Fragment f) {
        return (s, t) -> {
            // TODO(rosswang): search up the hierarchy
            final ErrorReporter erf = ErrorReporterFragment.find(f.getFragmentManager());
            if (erf == null) {
                log.error(f.getString(s), t);
            } else {
                erf.onError(s, t);
            }
        };
    }

    /**
     * Derives a default sync error reporting function from a {@link VAndroidContextTrait}. The
     * error message is {@link io.v.baku.toolkit.R.string#err_sync}.
     *
     * @see #getDefaultSyncErrorReporter(ErrorReporter)
     */
    public static Action1<Throwable> getDefaultSyncErrorReporter(final VAndroidContextTrait<?> v) {
        return getDefaultSyncErrorReporter(v.getErrorReporter());
    }

    /**
     * Derives a default sync error reporting function from an {@link ErrorReporter}. The error
     * message is {@link io.v.baku.toolkit.R.string#err_sync}.
     */
    public static Action1<Throwable> getDefaultSyncErrorReporter(final ErrorReporter r) {
        return t -> r.onError(R.string.err_sync, t);
    }
}
