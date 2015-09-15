// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.syncbase.util;

import com.google.common.base.Charsets;

import java.io.UnsupportedEncodingException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;

import io.v.impl.google.naming.NamingUtil;
import io.v.v23.V;
import io.v.v23.context.VContext;
import io.v.v23.namespace.Namespace;
import io.v.v23.naming.GlobReply;
import io.v.v23.verror.VException;

/**
 * Various NoSQL utility methods.
 */
public class Util {
    public static final String NAME_SEP = "$";
    public static final String NAME_SEP_WITH_SLASHES = "/$/";

    /**
     * Returns the start of the row range for the given prefix.
     */
    public static String prefixRangeStart(String prefix) {
        return prefix;
    }

    /**
     * Returns the limit of the row range for the given prefix.
     */
    public static String prefixRangeLimit(String prefix) {
        // We convert a string to a byte[] array, which can be thought of as a base-256
        // number.  The code below effectively adds 1 to this number, then chops off any
        // trailing 0x00 bytes. If the input string consists entirely of 0xFF, an empty string
        // will be returned.
        try {
            byte[] bytes = prefix.getBytes("ISO8859-1");
            int last = bytes.length - 1;
            for (; last >= 0 && bytes[last] == (byte) 0xFF; --last);
            if (last < 0) {
                return "";
            }
            bytes[last] += 1;
            return new String(bytes, 0, last + 1, "ISO8859-1");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("JVM must support ISO8859-1 char encoding", e);
        }
    }

    /**
     * Returns the UTF-8 encoding of the provided string.
     */
    public static byte[] getBytes(String s) {
        if (s == null) {
            s = "";
        }
        return s.getBytes(Charsets.UTF_8);
    }

    /**
     * Returns the UTF-8 decoded string.
     */
    public static String getString(byte[] bytes) {
        return new String(bytes, Charsets.UTF_8);
    }

    /**
     * Returns {@code true} iff the the given Syncbase component name (i.e. app,
     * database, table, or row name) is valid. Component names:
     * <p><ul>
     * <li>must be valid UTF-8;</li>
     * <li>must not contain {@code "\0"} or {@code "@@"};</li>
     * <li>must not have any slash-separated parts equal to {@code ""} or {@code "$"}; and</li>
     * <li>must not have any slash-separated parts that start with {@code "__"}.</li>
     * </ul><p>
     */
    public static boolean isValidName(String name) {
        // TODO(sadovsky): Check that name is valid UTF-8.
        if (name.contains("\0") || name.contains("@@")) {
            return false;
        }
        String[] parts = name.split("/");
        for (String v : parts) {
            if (v.isEmpty() || v.equals(NAME_SEP) || v.startsWith("__")) {
                return false;
            }
        }
        return true;
    }

    private Util() {}
}
