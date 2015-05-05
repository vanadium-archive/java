// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.x.jni.test.fortune;

import com.google.common.collect.ImmutableList;

import junit.framework.TestCase;

import org.joda.time.Duration;
import java.io.EOFException;
import java.util.List;
import io.v.v23.InputChannel;
import io.v.v23.OutputChannel;
import io.v.v23.V;
import io.v.v23.context.VContext;
import io.v.v23.naming.GlobError;
import io.v.v23.naming.GlobReply;
import io.v.v23.naming.MountEntry;
import io.v.v23.naming.MountedServer;
import io.v.v23.rpc.Globber;
import io.v.v23.rpc.NetworkChange;
import io.v.v23.rpc.Server;
import io.v.v23.rpc.ServerCall;
import io.v.v23.vdl.ClientStream;
import io.v.v23.vdl.Stream;
import io.v.v23.vdl.VdlUint32;
import io.v.v23.verror.VException;
import io.v.v23.vdlroot.signature.Interface;

import static com.google.common.truth.Truth.assertThat;

public class FortuneTest extends TestCase {
    static {
        V.init();
    }
    private static final ComplexErrorParam COMPLEX_PARAM = new ComplexErrorParam(
            "StrVal",
            11,
            ImmutableList.<VdlUint32>of(new VdlUint32(22), new VdlUint32(33)));

    private static final VException COMPLEX_ERROR = VException.explicitMake(
            Errors.ERR_COMPLEX, "en", "test", "test", COMPLEX_PARAM, "secondParam", 3);

    public static class FortuneServerImpl implements FortuneServer, Globber {
        private String lastAddedFortune;

        @Override
        public String get(VContext context, ServerCall call) throws VException {
            if (lastAddedFortune == null) {
                throw VException.make(Errors.ERR_NO_FORTUNES, context);
            }
            return lastAddedFortune;
        }

        @Override
        public void add(VContext context, ServerCall call, String fortune) throws VException {
            lastAddedFortune = fortune;
        }

        @Override
        public int streamingGet(VContext context, ServerCall call, Stream<String, Boolean> stream)
                throws VException {
            int numSent = 0;
            while (true) {
                try {
                    stream.recv();
                } catch (VException e) {
                    throw new VException(
                          "Server couldn't receive a boolean item: " + e.getMessage());
                } catch (EOFException e) {
                    break;
                }
                try {
                    stream.send(get(context, call));
                } catch (VException e) {
                    throw new VException(
                            "Server couldn't send a string item: " + e.getMessage());
                }
                ++numSent;
            }
            return numSent;
        }

        @Override
        public void getComplexError(VContext context, ServerCall call) throws VException {
            throw COMPLEX_ERROR;
        }

        @Override
        public void testServerCall(VContext context, ServerCall call) throws VException {
            if (call == null) {
                throw new VException("ServerCall is null");
            }
            if (call.suffix() == null) {
                throw new VException("Suffix is null");
            }
            if (call.localEndpoint() == null || call.localEndpoint().isEmpty()) {
                throw new VException("Local endpoint is empty");
            }
            if (call.remoteEndpoint() == null || call.remoteEndpoint().isEmpty()) {
                throw new VException("Remote endpoint is empty");
            }
            if (context == null) {
                throw new VException("Vanadium context is null");
            }
        }

        @Override
        public void noTags(VContext context, ServerCall call) throws VException {}

        @Override
        public void glob(ServerCall call, String pattern, OutputChannel<GlobReply> response)
                throws VException {
            final GlobReply.Entry entry = new GlobReply.Entry(
                    new MountEntry("helloworld", ImmutableList.<MountedServer>of(), false, false));
            response.writeValue(entry);
            final GlobReply.Error error = new GlobReply.Error(
                    new GlobError("Hello, world!", new VException("Some error")));
            response.writeValue(error);
            response.close();
        }
    }

    public void testFortune() throws VException {
        final VContext ctx = V.init();
        final Server s = V.newServer(ctx);
        final String[] endpoints = s.listen(V.getListenSpec(ctx));
        final FortuneServer server = new FortuneServerImpl();
        s.serve("", server);

        final String name = "/" + endpoints[0];
        final FortuneClient client = FortuneClientFactory.bind(name);
        final VContext ctxT = ctx.withTimeout(new Duration(20000)); // 20s
        try {
            client.get(ctxT);
            fail("Expected exception during call to get() before call to add()");
        } catch (VException e) {
            if (!e.is(Errors.ERR_NO_FORTUNES)) {
                fail(String.format("Expected error %s, got %s", Errors.ERR_NO_FORTUNES, e));
            }
        }
        final String firstMessage = "First fortune";
        client.add(ctxT, firstMessage);
        assertEquals(firstMessage, client.get(ctxT));
        s.stop();
    }

    public void testStreaming() throws VException {
        final VContext ctx = V.init();
        final Server s = V.newServer(ctx);
        final String[] endpoints = s.listen(V.getListenSpec(ctx));
        final FortuneServer server = new FortuneServerImpl();
        s.serve("", server);

        final String name = "/" + endpoints[0];
        final FortuneClient client = FortuneClientFactory.bind(name);
        final VContext ctxT = ctx.withTimeout(new Duration(20000));  // 20s
        final ClientStream<Boolean, String, Integer> stream = client.streamingGet(ctxT);
        final String msg = "The only fortune";
        client.add(ctxT, msg);
        try {
            for (int i = 0; i < 5; ++i) {
                stream.send(true);
                assertEquals(msg, stream.recv());
            }
        } catch (EOFException e) {
            fail("Reached unexpected stream EOF: " + e.getMessage());
        }
        final int total = stream.finish();
        assertEquals(5, total);
        s.stop();
    }

    public void testComplexError() throws VException {
        final VContext ctx = V.init();
        final Server s = V.newServer(ctx);
        final String[] endpoints = s.listen(V.getListenSpec(ctx));
        final FortuneServer server = new FortuneServerImpl();
        s.serve("", server);

        final String name = "/" + endpoints[0];
        final FortuneClient client = FortuneClientFactory.bind(name);
        final VContext ctxT = ctx.withTimeout(new Duration(20000)); // 20s
        try {
            client.getComplexError(ctxT);
            fail("Expected exception during call to getComplexError()");
        } catch (VException e) {
            if (!COMPLEX_ERROR.deepEquals(e)) {
                fail(String.format("Expected error %s, got %s", COMPLEX_ERROR, e));
            }
        }
        s.stop();
    }

    public void testWatchNetwork() throws VException {
        final VContext ctx = V.init();
        final Server s = V.newServer(ctx);
        s.listen(V.getListenSpec(ctx));
        final FortuneServer server = new FortuneServerImpl();
        s.serve("", server);

        // TODO(spetrovic): Figure out how to force network change in android and test that the
        // changes get announced on this channel.
        final InputChannel<NetworkChange> channel = s.watchNetwork();
        s.unwatchNetwork(channel);
    }

    public void testContext() throws VException {
        final VContext ctx = V.init();
        final Server s = V.newServer(ctx);
        final String[] endpoints = s.listen(V.getListenSpec(ctx));
        final FortuneServer server = new FortuneServerImpl();
        s.serve("", server);

        final String name = "/" + endpoints[0];
        final FortuneClient client = FortuneClientFactory.bind(name);
        final VContext ctxT = ctx.withTimeout(new Duration(20000)); // 20s
        try {
            client.testServerCall(ctxT);
        } catch (VException e) {
            fail("Context check failed: " + e.getMessage());
        }
    }

    public void testGetSignature() throws VException {
        final VContext ctx = V.init();
        final Server s = V.newServer(ctx);
        final String[] endpoints = s.listen(V.getListenSpec(ctx));
        final FortuneServer server = new FortuneServerImpl();
        s.serve("", server);

        final String name = "/" + endpoints[0];
        final FortuneClient client = FortuneClientFactory.bind(name);
        final VContext ctxT = ctx.withTimeout(new Duration(20000)); // 20s
        final Interface signature = client.getSignature(ctxT);
        assertThat(signature.getMethods()).isNotEmpty();
    }

    public void testGlob() throws VException {
        final VContext ctx = V.init();
        final Server s = V.newServer(ctx);
        final String[] endpoints = s.listen(V.getListenSpec(ctx));
        final FortuneServer server = new FortuneServerImpl();
        s.serve("", server);

        final String name = "/" + endpoints[0];
        final List<GlobReply> globResult
                = ImmutableList.copyOf(V.getNamespace(ctx).glob(ctx, name + "/*"));
        assertThat(globResult).hasSize(2);
        assertThat(globResult.get(0)).isInstanceOf(GlobReply.Entry.class);
        assertThat(((GlobReply.Entry) globResult.get(0)).getElem().getName())
                .isEqualTo(name + "/helloworld");
        assertThat(globResult.get(1)).isInstanceOf(GlobReply.Error.class);
    }
}
