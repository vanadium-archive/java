// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.namespace_browser;

import com.google.common.collect.ImmutableList;

import org.joda.time.Duration;

import java.util.List;

import io.v.v23.InputChannel;
import io.v.v23.android.V;
import io.v.v23.context.VContext;
import io.v.v23.naming.GlobReply;
import io.v.v23.verror.VException;

/**
 * Namespace provides utility methods for Veyron namespace.
 */
public class Namespace {
    /**
     * Returns the list of entries mounted under the provided namespace root.
     *
     * @param root root of the namespace
     * @return list of entries mounted under the provided root.
     * @throws VException if there was an error fetching the entries.
     */
    public static List<GlobReply> glob(String root, VContext ctx) throws VException {
        final io.v.v23.namespace.Namespace n = V.getNamespace(ctx);
        final VContext ctxT = ctx.withTimeout(new Duration(20000));  // 20s
        final InputChannel<GlobReply> chan = n.glob(ctxT, root.isEmpty() ? "*" : root + "/*");
        return ImmutableList.copyOf(chan);
    }
}
