// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit;

import android.app.Fragment;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

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
}
