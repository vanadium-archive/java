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
     * Performs {@link Namespace#glob Namespace.glob("name/*")} and returns a
     * sorted list of results.
     *
     * @param  ctx        Vanadium context
     * @param  globName   name used for globbing
     * @return            a sorted list of results of
     *                    {@link Namespace#glob Namespace.glob("name/*")}
     * @throws VException if a glob error occurred
     */
    public static String[] list(VContext ctx, String globName) throws VException {
        Namespace n = V.getNamespace(ctx);
        ArrayList<String> names = new ArrayList<String>();
        try {
            for (GlobReply reply : n.glob(ctx, NamingUtil.join(globName, "*"))) {
                if (reply instanceof GlobReply.Entry) {
                    String fullName = ((GlobReply.Entry) reply).getElem().getName();
                    // NOTE(nlacasse): The names that come back from Glob are all
                    // rooted.  We only want the last part of the name, so we must chop
                    // off everything before the final '/'.  Since endpoints can
                    // themselves contain slashes, we have to remove the endpoint from
                    // the name first.
                    String name = NamingUtil.splitAddressName(fullName).get(1);
                    int idx = name.lastIndexOf('/');
                    if (idx != -1) {
                        name = name.substring(idx + 1, name.length());
                    }
                    names.add(name);
                } else if (reply instanceof GlobReply.Error) {
                    throw ((GlobReply.Error) reply).getElem().getError();
                } else if (reply == null) {
                    throw new VException("null glob() reply");
                } else {
                    throw new VException("Unrecognized glob() reply type: " + reply.getClass());
                }
            }
        } catch (RuntimeException e) {  // error during iteration
            throw (VException) e.getCause();
        }
        Collections.sort(names, Collator.getInstance());
        return names.toArray(new String[names.size()]);
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

    private Util() {}
}
