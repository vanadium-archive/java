// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.rpc;

import io.v.v23.OutputChannel;
import io.v.v23.naming.GlobReply;
import io.v.v23.verror.VException;

/**
 * Servers that implement the {@code Globber} interface may take part in the Vanadium namespace.
 */
public interface Globber {
    /**
     * Invoked by the {@code VDLInvoker} in response to a glob request. The implementing class may
     * respond by writing zero or more {@link GlobReply} instances to the given {@code response}
     * channel. Once the replies are written, the implementing class <strong>must</strong> {@link
     * io.v.v23.OutputChannel#close close} the response channel.
     *
     * @param call     in-flight call information
     * @param pattern  the glob pattern from the client
     * @param response the channel to which the response must be written
     * @throws VException  if any errors occur while writing the response to the response channel
     */
    void glob(ServerCall call, String pattern, OutputChannel<GlobReply> response) throws VException;
}
