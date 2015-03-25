// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.i18n;

import android.annotation.TargetApi;
import android.os.Build;
import android.test.AndroidTestCase;

import io.v.v23.android.V;
import io.v.v23.context.VContext;
import io.v.v23.context.VContextImpl;

/**
 * Tests for the various Language utility methods.
 */
public class LanguageTest extends AndroidTestCase {
    static {
        V.init();
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    public void testLanguageFromContext() {
        final VContext dcWithoutLang = VContextImpl.create();
        final VContext dcWithEN = Language.contextWithLanguage(dcWithoutLang, "en");
        final VContext dcWithFR = Language.contextWithLanguage(dcWithEN, "fr");
        String got = "";

        got = Language.languageFromContext(dcWithoutLang);
        if (!got.isEmpty()) {
            fail(String.format("languageFromContext(dcWithoutLangID); got %v, want \"\"", got));
        }

        got = Language.languageFromContext(dcWithEN);
        if (!got.equals("en")) {
            fail(String.format("Language.languageFromContext(dcWithEN); got %v, want \"en\"", got));
        }

        got = Language.languageFromContext(dcWithFR);
        if (!got.equals("fr")) {
            fail(String.format("Language.languageFromContext(dcWithFR); got %v, want \"fr\"", got));
        }
    }

    public void testBaseLanguage() {
        expectBaseLanguage("en", "en");
        expectBaseLanguage("en-US", "en");
    }

    private void expectBaseLanguage(String lang, String want) {
        final String got = Language.baseLanguage(lang);
        if (!got.equals(want)) {
            fail(String.format("baseLanguage(%s) got %s, want %s", lang, got, want));
        }
    }
}
