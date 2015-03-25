// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.i18n;

import android.test.AndroidTestCase;

import io.v.v23.V;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.HashSet;
import java.util.Set;

/**
 * CatalogTest tests the catalog implementation.
 */
public class CatalogTest extends AndroidTestCase {
    static {
        V.init();
    }

    public void testLookupSetAndRemove() {
        final String want = "expected format";
        final Catalog cat = new Catalog();
        expectLookup(cat, "", "en", "bar", 1);
        expectLookup(cat, "", "en-US", "bar", 2);
        expectLookup(cat, "", "en", "foo", 3);
        expectLookup(cat, "", "en-US", "foo", 4);
        if (!cat.set("en-US", "bar", want).isEmpty()) {
            fail("set() found en-US/bar");
        }
        if (!cat.setWithBase("en-US", "foo", want).isEmpty()) {
            fail("set() found en-US/foo");
        }
        expectLookup(cat, "", "en", "bar", 5);
        expectLookup(cat, want, "en-US", "bar", 6);
        expectLookup(cat, want, "en", "foo", 7);
        expectLookup(cat, want, "en-US", "foo", 8);
        // Check that set(..., "") doesn't delete the base entry.
        if (!cat.set("en", "bar", "other format").isEmpty()) {
            fail("set found en/bar");
        }
        if (!cat.set("en-US", "bar", "").equals(want)) {
            fail("set didn't find en-US/bar");
        }
        if (!cat.setWithBase("en-US", "foo", "").equals(want)) {
            fail("set didn't find en-US/foo");
        }
        // The previous setWithBase will not have removed the base entry.
        if (!cat.set("en", "foo", "").equals(want)) {
            fail("set didn't find en/foo");
        }
        expectLookup(cat, "other format", "en", "bar", 9);
        // Test that a lookup of en-US finds the "en" entry.
        expectLookup(cat, "other format", "en-US", "bar", 10);
        if (!cat.set("en", "bar", "").equals("other format")) {
            fail("set didn't find en/bar");
        }
        expectLookup(cat, "", "en", "bar", 11);
        expectLookup(cat, "", "en-US", "bar", 12);
        expectLookup(cat, "", "en", "foo", 13);
        expectLookup(cat, "", "en-US", "foo", 14);
    }

    public void testFormatParams() {
        expectFormatParams("", "", "1st");
        expectFormatParams("", "{_}");
        expectFormatParams("? ? ? ? ? ?", "{0} {1} {2} {3} {4} {5}");
        expectFormatParams("{ foo }?", "{ foo }{2}");
        expectFormatParams("3rd: foo 2nd bar 1st 4th (3rd)",
            "{3}: foo {2} bar {_} ({3})", "1st", "2nd", "3rd", "4th");
        expectFormatParams("?: foo 4th ?", "{0}: foo {4} {5}", "1st", "2nd", "3rd", "4th");
        expectFormatParams(" foo 1st 2nd 3rd 4th{-1}",
                "{_} foo {_}{-1}", "1st", "2nd", "3rd", "4th");
        expectFormatParams("{ foo }2nd", "{ foo }{2}", "1st", "2nd", "3rd", "4th");

        // Test the formatting of colon-formats.
        expectFormatParams("", "{:_}");
        expectFormatParams("", "{_:}");
        expectFormatParams("", "{:_:}");

        expectFormatParams(": 1st 2nd", "{:_}", "1st", "2nd");
        expectFormatParams("1st 2nd:", "{_:}", "1st", "2nd");
        expectFormatParams(": 1st 2nd:", "{:_:}", "1st", "2nd");

        expectFormatParams("", "{:_}", "");
        expectFormatParams("", "{_:}", "");
        expectFormatParams("", "{:_:}", "");

        expectFormatParams(": 1st", "{:1}", "1st");
        expectFormatParams("1st:", "{1:}", "1st");
        expectFormatParams(": 1st:", "{:1:}", "1st");

        expectFormatParams("", "{:1}", "");
        expectFormatParams("", "{1:}", "");
        expectFormatParams("", "{:1:}", "");

        expectFormatParams("?: ? ?: ?: ?: ?", "{0}{:1} {2:} {3}{:4:} {5}");

        expectFormatParams("{: foo }?", "{: foo }{2}");
        expectFormatParams("{ foo :}?", "{ foo :}{2}");
        expectFormatParams("{: foo :}?", "{: foo :}{2}");

        expectFormatParams("3rd: foo 2nd bar: 1st 4th (3rd)",
            "{3:} foo {2} bar{:_} ({3})", "1st", "2nd", "3rd", "4th");

        expectFormatParams("?: foo: 4th ?",
            "{0:} foo{:4} {5}", "1st", "2nd", "3rd", "4th");

        expectFormatParams(" foo: 1st 2nd 3rd 4th{-1}",
            "{_:} foo{:_}{-1}", "1st", "2nd", "3rd", "4th");

        expectFormatParams("{ foo }: 2nd",
            "{4:}{ foo }{:2}", "1st", "2nd", "3rd", "");

        expectFormatParams("1st foo 2nd: bar: 3rd wombat: 4th: numbat",
            "{1} foo {2:} bar{:3} wombat{:4:} numbat",
            "1st", "2nd", "3rd", "4th");

        expectFormatParams(" foo  bar wombat numbat",
            "{1} foo {2:} bar{:3} wombat{:4:} numbat",
            "", "", "", "");
    }

    private static String MERGE_DATA =
            "# In what follows we use the \"languages\" \"fwd\" and \"back\".\n" +
            "fwd foo \"{1} foo to {2}\"\n" +
            "# Next line has a missing trailing double quote, so will be ignored.\n" +
            " back   foo   \"{2} from foo {1}\n" +
            "\n" +
            "# Comment \"quote\"\n" +
            "\n" +
            "# The following two lines are ignored, since each has fewer than three tokens.\n" +
            "one" +
            "one two\n" +
            "\n" +
            "fwd   bar \"{1} bar to {2}\"\n" +
            "back bar \"{2} from bar {1}\" extraneous word\n" +
            "\n" +
            "back funny.msg.id \"{2} from funny msg id {1}\"\n" +
            "odd.lang.id funny.msg.id \"odd and\\b \\\"funny\\\"\"";

    public void testMergeAndOutput() {
        final Catalog cat = new Catalog();

        // Check that Merge() works.
        try {
            cat.merge(new ByteArrayInputStream(MERGE_DATA.getBytes("UTF-8")));
        } catch (IOException e) {
            fail("Error merging input: " + e.getMessage());
        }
        expectLookup(cat, "{1} foo to {2}", "fwd", "foo", 1);
        expectLookup(cat, "", "back", "foo", 2);
        expectLookup(cat, "{1} bar to {2}", "fwd", "bar", 3);
        expectLookup(cat, "{2} from bar {1}", "back", "bar", 4);
        expectLookup(cat, "{2} from funny msg id {1}", "back", "funny.msg.id", 5);
        expectLookup(cat, "odd and\\b \\\"funny\\\"", "odd.lang.id", "funny.msg.id", 6);

        // Verify that the result of Output is as expected.
        final PipedInputStream in = new PipedInputStream();
        PipedOutputStream out = null;
        try {
            out = new PipedOutputStream(in);
            cat.output(out);
        } catch (IOException e) {
            fail("Error writing output: " + e.getMessage());
        }

        final Set<String> lines = new HashSet<String>();
        final BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            fail("Unexpected exception while reading the output: " + e.getMessage());
        }
        expectInSet(lines, "fwd foo \"{1} foo to {2}\"");
        expectInSet(lines, "fwd bar \"{1} bar to {2}\"");
        expectInSet(lines, "back bar \"{2} from bar {1}\"");
        expectInSet(lines, "back funny.msg.id \"{2} from funny msg id {1}\"");
        expectInSet(lines, "odd.lang.id funny.msg.id \"odd and\\b \\\"funny\\\"\"");
        if (lines.size() != 5) {
            fail(String.format(
                    "Wrong number of lines in <%s>; got %d, want 5", lines, lines.size()));
        }
    }

    private void expectLookup(Catalog cat, String want, String language, String msgID, int tag) {
        final String got = cat.lookup(language, msgID);
        if (!want.equals(got)) {
            fail(String.format(
                    "%d: cat.lookup(%s, %s): got %s, want %s", tag, language, msgID, got, want));
        }
    }

    private void expectFormatParams(String want, String format, Object... params) {
        final String got = Catalog.formatParams(format, params);
        if (!want.equals(got)) {
            fail(String.format("formatParams(%s, %s): got %s, want %s", format, params, got, want));
        }
    }

    private void expectInSet(Set<String> set, String elem) {
        if (!set.contains(elem)) {
            fail(String.format("Set %s doesn't contain element: %s", set, elem));
        }
    }
}
