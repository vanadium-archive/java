// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.i18n;

import junit.framework.TestCase;

import io.v.v23.V;
import io.v.v23.context.VContext;
import io.v.v23.context.VContextImpl;

/**
 * Tests for the various Language utility methods.
 */
public class LanguageTest extends TestCase {
    static {
        V.init();
    }

    public void testLanguageFromContext() {
        VContext dcWithoutLang = VContextImpl.create();
        VContext dcWithEN = Language.contextWithLanguage(dcWithoutLang, "en");
        VContext dcWithFR = Language.contextWithLanguage(dcWithEN, "fr");
        assertTrue(Language.languageFromContext(dcWithoutLang).isEmpty());
        assertEquals("en", Language.languageFromContext(dcWithEN));
        assertEquals("fr", Language.languageFromContext(dcWithFR));
    }

    public void testBaseLanguage() {
        assertEquals("en", Language.baseLanguage("en"));
        assertEquals("en", Language.baseLanguage("en-US"));
    }
}
