package io.v.v23.security;

import android.test.AndroidTestCase;

import org.joda.time.DateTime;

import io.v.v23.V;
import io.v.v23.vdl.VdlString;
import io.v.v23.vdl.VdlUint32;
import io.v.v23.vdl.VdlValue;
import io.v.v23.verror.VException;

/**
 * Tests the VContext implementations.
 */
public class VContextTest extends AndroidTestCase {
    public void testContextParams() throws VException {
        final io.v.v23.context.VContext vCtx = V.init();
        final DateTime timestamp = new DateTime();
        final String method = "bono";
        final VdlValue[] methodTags = { new VdlUint32(12), new VdlString("edge") };
        final String suffix = "larry";
        final Principal principal = Security.newPrincipal();
        final Blessings localBlessings = principal.blessSelf("adam");
        final Blessings remoteBlessings = principal.blessSelf("u2");
        final String localEndpoint = "@3@tcp@10.0.0.0:1000@";
        final String remoteEndpoint = "@3@tcp@10.1.1.1:1111@";
        final VContextParams params = new VContextParams()
                .withTimestamp(timestamp)
                .withMethod(method)
                .withMethodTags(methodTags)
                .withSuffix(suffix)
                .withLocalEndpoint(localEndpoint)
                .withRemoteEndpoint(remoteEndpoint)
                .withLocalPrincipal(principal)
                .withLocalBlessings(localBlessings)
                .withRemoteBlessings(remoteBlessings)
                .withContext(vCtx);
        final VContext ctx = Security.newContext(params);
        assertEquals(timestamp, ctx.timestamp());
        assertEquals(method, ctx.method());
        assertEquals(methodTags, ctx.methodTags());
        assertEquals(suffix, ctx.suffix());
        assertEquals(principal, ctx.localPrincipal());
        assertEquals(localBlessings, ctx.localBlessings());
        assertEquals(remoteBlessings, ctx.remoteBlessings());
        assertEquals(localEndpoint, ctx.localEndpoint());
        assertEquals(remoteEndpoint, ctx.remoteEndpoint());
        assertEquals(vCtx, ctx.context());
    }
}
