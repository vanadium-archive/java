// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.syncslides.discovery;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.List;

/**
 * Generates a string for use as a V23 Service Name based on the current time
 * down to millisecond resolution. Ugly name, but very unlikely to collide.
 */
class NameGeneratorByDate implements NameGenerator {
    private static final DateTimeFormatter FMT =
            DateTimeFormat.forPattern("yyyy_MM_dd_hh_mm_ss_SSSS");

    /**
     * Ignore incoming data, just pick 'now' down to the millisecond.
     */
    @Override
    public String getName(List<String> existing, String suggested) {
        return DateTime.now().toString(FMT);
    }
}
