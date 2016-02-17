// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.rx.syncbase;

import com.google.common.escape.CharEscaperBuilder;
import com.google.common.escape.Escaper;

import lombok.experimental.UtilityClass;

@UtilityClass
public class SgSuffixFormats {
    public static final String SG_SUFFIX_DELIMITER = "/";
    public static final Escaper SG_SUFFIX_COMPONENT_ESCAPER = new CharEscaperBuilder()
            .addEscape('/', "\\/")
            .addEscape('\\', "\\\\")
            .toEscaper();

    public static SgSuffixFormat<Object> simple(final String sgSuffix) {
        return x -> sgSuffix;
    }

    public static SgSuffixFormat<UserSyncgroup.Parameters> discriminated(
            final String customSuffix) {
        return p -> SG_SUFFIX_COMPONENT_ESCAPER.escape(p.getDb().getRxApp().getName()) +
                SG_SUFFIX_DELIMITER +
                SG_SUFFIX_COMPONENT_ESCAPER.escape(p.getDb().getName()) +
                SG_SUFFIX_DELIMITER +
                customSuffix;
    }
}
