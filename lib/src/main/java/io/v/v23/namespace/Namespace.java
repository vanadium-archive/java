// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.namespace;

import io.v.v23.InputChannel;
import io.v.v23.context.VContext;
import io.v.v23.naming.GlobReply;
import io.v.v23.verror.VException;

/**
 * Namespace provides translation from object names to server object addresses.  It represents the
 * interface to a client side library for the MountTable service.
 */
public interface Namespace {
    /**
     * Returns all names matching the provided pattern.
     *
     * @param  context         a client context.
     * @param  pattern         a pattern that should be matched.
     * @return                 an input channel of GlobReply objects matching the provided pattern.
     * @throws VException      if an error is encountered.
     */
    InputChannel<GlobReply> glob(VContext context, String pattern) throws VException;
}
