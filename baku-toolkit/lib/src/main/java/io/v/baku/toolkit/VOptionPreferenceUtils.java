// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit;

import android.content.SharedPreferences;

import com.google.common.base.Strings;

import io.v.v23.OptionDefs;
import io.v.v23.Options;
import java8.util.Optional;
import lombok.experimental.UtilityClass;

@UtilityClass
public class VOptionPreferenceUtils {
    public static Optional<Integer> readVLevel(final SharedPreferences prefs) {
        final String raw = prefs.getString(OptionDefs.LOG_VLEVEL, "");
        try {
            return Optional.of(Integer.parseInt(raw));
        } catch (final NumberFormatException|NullPointerException e) {
            return Optional.empty();
        }
    }

    public static Optional<String> readVModule(final SharedPreferences prefs) {
        final String raw = prefs.getString(OptionDefs.LOG_VMODULE, "");
        if (Strings.isNullOrEmpty(raw)) {
            return Optional.empty();
        } else {
            return Optional.of(raw);
        }
    }

    public static Options getOptionsFromPreferences(final SharedPreferences prefs) {
        final Options opts = new Options();
        /* It would be nice to copy this map naively, but Vanadium options are type-specific and
        Android stores some numeric preferences as strings. */
        readVLevel(prefs).ifPresent(v -> opts.set(OptionDefs.LOG_VLEVEL, v));
        readVModule(prefs).ifPresent(v -> opts.set(OptionDefs.LOG_VMODULE, v));
        return opts;
    }
}
