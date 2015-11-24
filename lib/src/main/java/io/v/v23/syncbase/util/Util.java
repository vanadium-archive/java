// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.syncbase.util;

import com.google.common.base.Charsets;
import com.google.common.collect.Ordering;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import io.v.impl.google.naming.NamingUtil;
import io.v.v23.V;
import io.v.v23.VIterable;
import io.v.v23.context.VContext;
import io.v.v23.naming.GlobReply;
import io.v.v23.verror.VException;

import java.io.UnsupportedEncodingException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.List;

/**
 * Various NoSQL utility methods.
 */
public class Util {
    /**
     * Escapes a component name for use in a Syncbase object name. In
     * particular, it replaces bytes {@code "%"} and {@code "/"} with the
     * {@code "%"} character followed by the byte's two-digit hex code. Clients
     * using the client library need not escape names themselves; the client
     * library does so on their behalf.
     *
     * @param s String to escape.
     * @return Escaped string.
     */
    public static String escape(String s) {
      return NamingUtil.encodeAsNameElement(s);
    }

    /**
     * Applies the inverse of escape. Throws exception if the given string is
     * not a valid escaped string.
     *
     * @param s String to unescape.
     * @return Unescaped string.
     * @throws IllegalArgumentException if {@code s} is truncated or malformed.
     */
    public static String unescape(String s) {
      return NamingUtil.decodeFromNameElement(s);
    }

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
     * Returns a new {@link ListenableFuture} whose result are the relative names of all children
     * of {@code parentFullName}.
     *
     * @param  ctx            Vanadium context
     * @param  parentFullName object name of parent component
     */
    public static ListenableFuture<List<String>> listChildren(VContext ctx, String parentFullName) {
        final ListenableFuture<VIterable<GlobReply>> it =
                V.getNamespace(ctx).glob(ctx, NamingUtil.join(parentFullName, "*"));
        return Futures.transform(it, new AsyncFunction<VIterable<GlobReply>, List<String>>() {
            @Override
            public ListenableFuture<List<String>> apply(VIterable<GlobReply> result) throws
                    Exception {
                // NOTE(spetrovic): This code should change once we handle the streaming RPC
                // in a truly asynchronous way.  What's the point of returning an iterator
                // that will immediately block?
                List<String> names = new ArrayList<>();
                for (GlobReply reply : result) {
                    if (reply instanceof GlobReply.Entry) {
                        String fullName = ((GlobReply.Entry) reply).getElem().getName();
                        int idx = fullName.lastIndexOf('/');
                        if (idx == -1) {
                            throw new VException("Unexpected glob() reply name: " + fullName);
                        }
                        String escName = fullName.substring(idx + 1, fullName.length());
                        // Component names within object names are always escaped.
                        // See comment in server/nosql/dispatcher.go for
                        // explanation. If unescape throws an exception, there's a
                        // bug in the Syncbase server. Glob should return names with
                        // escaped components.
                        names.add(unescape(escName));
                    } else if (reply instanceof GlobReply.Error) {
                        // TODO(sadovsky): Surface these errors somehow. (We don't
                        // want to throw an exception, since some names may simply
                        // be hidden to this client.)
                    } else if (reply == null) {
                        throw new VException("null glob() reply");
                    } else {
                        throw new VException("Unrecognized glob() reply type: " + reply.getClass());
                    }
                }
                if (result.error() != null) {
                    // Error during iteration.
                    throw result.error();
                }
                return Futures.immediateFuture((List<String>)
                        Ordering.from(Collator.getInstance()).immutableSortedCopy(names));
            }
        });
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
