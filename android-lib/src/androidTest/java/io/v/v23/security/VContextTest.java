// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.security;

import android.test.AndroidTestCase;

import org.joda.time.DateTime;

import io.v.v23.V;
import io.v.v23.context.VContext;
import io.v.v23.vdl.VdlString;
import io.v.v23.vdl.VdlUint32;
import io.v.v23.vdl.VdlValue;
import io.v.v23.verror.VException;

/**
 * Tests the VContext implementations.
 */
public class VContextTest extends AndroidTestCase {
    public void testContextParams() throws VException {
        final VContext ctx = V.init();
        final DateTime timestamp = new DateTime();
        final String method = "bono";
        final VdlValue[] methodTags = { new VdlUint32(12), new VdlString("edge") };
        final String suffix = "larry";
        final Principal principal = Security.newPrincipal();
        final Blessings localBlessings = principal.blessSelf("adam");
        final Blessings remoteBlessings = principal.blessSelf("u2");
        final String localEndpoint = "@3@tcp@10.0.0.0:1000@";
        final String remoteEndpoint = "@3@tcp@10.1.1.1:1111@";
        final CallParams params = new CallParams()
                .withTimestamp(timestamp)
                .withMethod(method)
                .withMethodTags(methodTags)
                .withSuffix(suffix)
                .withLocalEndpoint(localEndpoint)
                .withRemoteEndpoint(remoteEndpoint)
                .withLocalPrincipal(principal)
                .withLocalBlessings(localBlessings)
                .withRemoteBlessings(remoteBlessings)
                .withContext(ctx);
        final Call call = Security.newCall(params);
        assertEquals(timestamp, call.timestamp());
        assertEquals(method, call.method());
        assertEquals(methodTags, call.methodTags());
        assertEquals(suffix, call.suffix());
        assertEquals(principal, call.localPrincipal());
        assertEquals(localBlessings, call.localBlessings());
        assertEquals(remoteBlessings, call.remoteBlessings());
        assertEquals(localEndpoint, call.localEndpoint());
        assertEquals(remoteEndpoint, call.remoteEndpoint());
    }
}
