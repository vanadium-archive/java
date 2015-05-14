// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.rpc;

import io.v.v23.OutputChannel;
import io.v.v23.naming.GlobReply;
import io.v.v23.verror.VException;

/**
 * Interface that allows the object to enumerate the the entire namespace below the receiver object.
 * <p>
 * Every object that implements it must be able to handle glob requests that could match any object
 * below itself. E.g. {@code "a/b".glob("* /*")}, {@code "a/b".glob("c/...")}, etc.
 */
public interface Globber {
    /**
     * Handles a glob request. The implementing class may respond by writing zero or more
     * {@link GlobReply} instances to the given {@code response} channel. Once the replies are
     * written, the implementing class <strong>must</strong>
     * {@link io.v.v23.OutputChannel#close close} the response channel.
     *
     * @param call         in-flight call information
     * @param pattern      the glob pattern from the client
     * @param response     the channel to which the responses must be written
     * @throws VException  if any errors occur while writing the responses to the response channel
     */
    void glob(ServerCall call, String pattern, OutputChannel<GlobReply> response) throws VException;
}
