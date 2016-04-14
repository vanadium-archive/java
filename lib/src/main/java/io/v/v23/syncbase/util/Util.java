// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.syncbase.util;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.collect.Ordering;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import io.v.impl.google.naming.NamingUtil;
import io.v.v23.InputChannel;
import io.v.v23.InputChannels;
import io.v.v23.V;
import io.v.v23.VFutures;
import io.v.v23.context.VContext;
import io.v.v23.naming.GlobReply;
import io.v.v23.services.syncbase.Id;
import io.v.v23.verror.VException;

import java.io.UnsupportedEncodingException;
import java.text.Collator;
import java.util.Comparator;
import java.util.List;

import javax.annotation.CheckReturnValue;

/**
 * Various NoSQL utility methods.
 */
public class Util {
    private Util() {
    }

    /**
     * Encodes a component name for use in a Syncbase object name. In
     * particular, it replaces bytes {@code "%"} and {@code "/"} with the
     * {@code "%"} character followed by the byte's two-digit hex code. Clients
     * using the client library need not encode names themselves; the client
     * library does so on their behalf.
     *
     * @param s String to encode.
     * @return Encoded string.
     */
    public static String encode(String s) {
        return NamingUtil.encodeAsNameElement(s);
    }

    /**
     * Applies the inverse of {@link Util#encode}. Throws exception if the given string is
     * not a valid encoded string.
     *
     * @param s String to decode.
     * @return Decoded string.
     * @throws IllegalArgumentException if {@code s} is truncated or malformed.
     */
    public static String decode(String s) {
        return NamingUtil.decodeFromNameElement(s);
    }

    /**
     * EncodeId encodes the given Id for use in a Syncbase object name.
     *
     * @param id Id to encode.
     * @return Encoded string.
     */
    public static String encodeId(Id id) {
        // Note that "," is not allowed to appear in blessing patterns. We also
        // could've used "/" as a separator, but then we would've had to be more
        // careful with decoding and splitting name components elsewhere.
        return encode(id.getBlessing() + "," + id.getName());
    }

    /**
     * Applies the inverse of {@link Util#encodeId}. Throws exception if the given string is
     * not a valid encoded string.
     *
     * @param s String to dencode.
     * @return decoded string.
     * @throws IllegalArgumentException if {@code s} is truncated or malformed.
     */
    public static Id decodeId(String s) throws VException {
        String decoded = decode(s);
        String[] parts = decoded.split(",", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Failed to decode id: " + s);
        }
        Id id = new Id(parts[0], parts[1]);
        return id;
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
            for (; last >= 0 && bytes[last] == (byte) 0xFF; --last) ;
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
     * <p>
     * The returned future is guaranteed to be executed on an {@link java.util.concurrent.Executor}
     * specified in {@code context} (see {@link V#withExecutor}).
     * <p>
     * The returned future will fail with {@link java.util.concurrent.CancellationException} if
     * {@code context} gets canceled.
     *
     * @param context        Vanadium context
     * @param parentFullName object name of parent component
     */
    @CheckReturnValue
    public static ListenableFuture<List<Id>> listChildIds(
            VContext context, String parentFullName) {
        InputChannel<GlobReply> input =
                V.getNamespace(context).glob(context, NamingUtil.join(parentFullName, "*"));
        return VFutures.withUserLandChecks(context,
                Futures.transform(InputChannels.asList(InputChannels.transform(context, input,
                        new InputChannels.TransformFunction<GlobReply, Id>() {
                            @Override
                            public Id apply(GlobReply from) throws VException {
                                return idFromGlobReply(from);
                            }
                        })), new Function<List<Id>, List<Id>>() {
                    @Override
                    public List<Id> apply(List<Id> input) {
                        return Ordering.from(new IdComparator())
                                .immutableSortedCopy(input);
                    }
                }));
    }

    private static Id idFromGlobReply(GlobReply reply) throws VException {
        if (reply instanceof GlobReply.Entry) {
            String fullName = ((GlobReply.Entry) reply).getElem().getName();
            int idx = fullName.lastIndexOf('/');
            if (idx == -1) {
                throw new VException("Unexpected glob() reply name: " + fullName);
            }
            String encName = fullName.substring(idx + 1, fullName.length());
            // Component names within object names are always encoded.
            // See comment in server/nosql/dispatcher.go for
            // explanation. If decode throws an exception, there's a
            // bug in the Syncbase server. Glob should return names with
            // encoded components.
            return decodeId(encName);
        } else if (reply instanceof GlobReply.Error) {
            // TODO(sadovsky): Surface these errors somehow. (We don't
            // want to throw an exception, since some names may simply
            // be hidden to this client.)
            return null;
        } else if (reply == null) {
            throw new VException("null glob() reply");
        } else {
            throw new VException("Unrecognized glob() reply type: " + reply.getClass());
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
     * Returns the app blessing obtained from the context.
     */
    public static String AppBlessingFromContext(VContext ctx) {
        // NOTE(sadovsky): For now, we use a blessing string that will be easy to
        // find-replace when we actually implement this method.
        return "v.io:a:xyz";
    }

    /**
     * Returns the user blessing obtained from the context.
     */
    public static String UserBlessingFromContext(VContext ctx) {
        // NOTE(sadovsky): For now, we use a blessing string that will be easy to
        // find-replace when we actually implement this method.
        return "v.io:u:sam";
    }

    /**
     * Compares two Syncbase Ids.
     * Blessing is compared first and then the name.
     */
    private static class IdComparator implements Comparator<Id> {
        @Override
        public int compare(Id a, Id b) {
            final Collator collator = Collator.getInstance();
            if (a.getBlessing() != b.getBlessing()) {
                return collator.compare(a.getBlessing(), b.getBlessing());
            } else {
                return collator.compare(a.getName(), b.getName());
            }
        }
    }

}

