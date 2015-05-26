// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.namespace;

import io.v.v23.InputChannel;
import io.v.v23.Options;
import io.v.v23.context.VContext;
import io.v.v23.naming.GlobReply;
import io.v.v23.verror.VException;

/**
 * Translation from object names to server object addresses.  It represents the interface to a
 * client side library for the {@code MountTable} service.
 */
public interface Namespace {
    /**
     * Returns all names matching the provided pattern.
     *
     * @param  context         a client context
     * @param  pattern         a pattern that should be matched
     * @return                 an input channel of {@link GlobReply} objects matching the
     *                         provided pattern
     * @throws VException      if an error is encountered
     */
    InputChannel<GlobReply> glob(VContext context, String pattern) throws VException;

    /**
     * Same as {@link #glob(VContext,String)} but makes the call using the provided options.
     * <p>
     * A particular implementation of this interface chooses which options to support,
     * but at the minimum it must handle the following pre-defined options:
     * <ul>
     *     <li>{@link io.v.v23.OptionDefs#SKIP_SERVER_ENDPOINT_AUTHORIZATION}</li>
     * </ul>
     *
     * @param  context         a client context
     * @param  pattern         a pattern that should be matched
     * @param  opts            client options for the glob call
     * @return                 an input channel of {@link GlobReply} objects matching the
     *                         provided pattern
     * @throws VException      if an error is encountered
     */
    InputChannel<GlobReply> glob(VContext context, String pattern, Options opts) throws VException;
}