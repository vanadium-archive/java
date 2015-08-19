// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.x.jni.test.fortune;

import com.google.common.collect.ImmutableList;
import com.google.common.reflect.TypeToken;
import io.v.v23.OutputChannel;
import io.v.v23.V;
import io.v.v23.context.VContext;
import io.v.v23.naming.Endpoint;
import io.v.v23.naming.GlobReply;
import io.v.v23.rpc.Client;
import io.v.v23.rpc.ClientCall;
import io.v.v23.rpc.Dispatcher;
import io.v.v23.rpc.Invoker;
import io.v.v23.rpc.ListenSpec;
import io.v.v23.rpc.NetworkChange;
import io.v.v23.rpc.Server;
import io.v.v23.rpc.ServerCall;
import io.v.v23.rpc.ServiceObjectWithAuthorizer;
import io.v.v23.rpc.StreamServerCall;
import io.v.v23.vdl.TypedClientStream;
import io.v.v23.vdl.VdlValue;
import io.v.v23.vdlroot.signature.Interface;
import io.v.v23.vdlroot.signature.Method;
import io.v.v23.verror.VException;
import junit.framework.TestCase;
import org.joda.time.Duration;

import java.io.EOFException;
import java.lang.reflect.Type;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;

public class FortuneTest extends TestCase {
    private static final String TEST_INVOKER_FORTUNE = "Test invoker fortune";

    private Server s;
    private VContext ctx;
    private ListenSpec listenSpec;

    @Override
    protected void setUp() throws Exception {
        ctx = V.init();
        s = V.newServer(ctx);
        listenSpec = V.getListenSpec(ctx).withAddress(new ListenSpec.Address("tcp", "127.0.0.1:0"));
    }

    @Override
    protected void tearDown() throws Exception {
        if (s != null) {
            s.stop();
        }
        V.shutdown();
    }

    public void testFortune() throws Exception {
        Endpoint[] endpoints = s.listen(listenSpec);
        FortuneServer server = new FortuneServerImpl();
        s.serve("", server, null);

        String name = "/" + endpoints[0];
        FortuneClient client = FortuneClientFactory.getFortuneClient(name);
        VContext ctxT = ctx.withTimeout(new Duration(20000)); // 20s
        try {
            client.get(ctxT);
            fail("Expected exception during call to get() before call to add()");
        } catch (VException e) {
            if (!e.is(Errors.ERR_NO_FORTUNES)) {
                fail(String.format("Expected error %s, got %s", Errors.ERR_NO_FORTUNES, e));
            }
        }
        String firstMessage = "First fortune";
        client.add(ctxT, firstMessage);
        assertEquals(firstMessage, client.get(ctxT));
    }

    public void testStreaming() throws Exception {
        Endpoint[] endpoints = s.listen(listenSpec);
        FortuneServer server = new FortuneServerImpl();
        s.serve("", server, null);

        String name = "/" + endpoints[0];
        FortuneClient client = FortuneClientFactory.getFortuneClient(name);
        VContext ctxT = ctx.withTimeout(new Duration(20000));  // 20s
        TypedClientStream<Boolean, String, Integer> stream = client.streamingGet(ctxT);
        String msg = "The only fortune";
        client.add(ctxT, msg);
        try {
            for (int i = 0; i < 5; ++i) {
                stream.send(true);
                assertEquals(msg, stream.recv());
            }
        } catch (EOFException e) {
            fail("Reached unexpected stream EOF: " + e.getMessage());
        }
        int total = stream.finish();
        assertEquals(5, total);
    }

    public void testMultiple() throws Exception {
        Endpoint[] endpoints = s.listen(listenSpec);
        FortuneServer server = new FortuneServerImpl();
        s.serve("", server, null);

        String name = "/" + endpoints[0];
        FortuneClient client = FortuneClientFactory.getFortuneClient(name);
        VContext ctxT = ctx.withTimeout(new Duration(20000)); // 20s
        String firstMessage = "First fortune";
        client.add(ctxT, firstMessage);

        FortuneClient.MultipleGetOut ret = client.multipleGet(ctxT);
        assertEquals(firstMessage, ret.fortune);
        assertEquals(firstMessage, ret.another);
    }

    public void testComplexError() throws Exception {
        Endpoint[] endpoints = s.listen(listenSpec);
        FortuneServer server = new FortuneServerImpl();
        s.serve("", server, null);

        String name = "/" + endpoints[0];
        FortuneClient client = FortuneClientFactory.getFortuneClient(name);
        VContext ctxT = ctx.withTimeout(new Duration(20000)); // 20s
        try {
            client.getComplexError(ctxT);
            fail("Expected exception during call to getComplexError()");
        } catch (VException e) {
            if (!FortuneServerImpl.COMPLEX_ERROR.deepEquals(e)) {
                fail(String.format("Expected error %s, got %s", FortuneServerImpl.COMPLEX_ERROR, e));
            }
        }
    }

    public void testWatchNetwork() throws Exception {
        s.listen(listenSpec);
        FortuneServer server = new FortuneServerImpl();
        s.serve("", server, null);

        // TODO(spetrovic): Figure out how to force network change in android and test that the
        // changes get announced on this channel.
        Iterable<NetworkChange> channel = s.watchNetwork();
        s.unwatchNetwork(channel);
    }

    public void testContext() throws Exception {
        Endpoint[] endpoints = s.listen(listenSpec);
        FortuneServer server = new FortuneServerImpl();
        s.serve("", server, null);

        String name = "/" + endpoints[0];
        FortuneClient client = FortuneClientFactory.getFortuneClient(name);
        VContext ctxT = ctx.withTimeout(new Duration(20000)); // 20s
        try {
            client.testServerCall(ctxT);
        } catch (VException e) {
            fail("Context check failed: " + e.getMessage());
        }
    }

    public void testGetSignature() throws Exception {
        Endpoint[] endpoints = s.listen(listenSpec);
        FortuneServer server = new FortuneServerImpl();
        s.serve("", server, null);

        String name = "/" + endpoints[0];
        Client c = V.getClient(ctx);
        VContext ctxT = ctx.withTimeout(new Duration(20000)); // 20s
        ClientCall call = c.startCall(ctxT, name, "__Signature", new Object[0], new Type[0]);
        Object[] results = call.finish(new Type[] { new TypeToken<Interface[]>() {}.getType() });
        assertThat(results.length == 1).isTrue();
        Interface[] signature = (Interface[]) results[0];
        assertThat(signature.length >= 1).isTrue();
        assertThat(signature[0].getMethods()).isNotEmpty();
    }

    public void testGlob() throws Exception {
        Endpoint[] endpoints = s.listen(listenSpec);
        FortuneServer server = new FortuneServerImpl();
        s.serve("", server, null);

        String name = "/" + endpoints[0];
        List<GlobReply> globResult
                = ImmutableList.copyOf(V.getNamespace(ctx).glob(ctx, name + "/*"));
        assertThat(globResult).hasSize(2);
        assertThat(globResult.get(0)).isInstanceOf(GlobReply.Entry.class);
        assertThat(((GlobReply.Entry) globResult.get(0)).getElem().getName())
                .isEqualTo(name + "/helloworld");
        assertThat(globResult.get(1)).isInstanceOf(GlobReply.Error.class);
    }

    public void testCustomInvoker() throws Exception {
        Endpoint[] endpoints = s.listen(listenSpec);
        s.serve("", new TestInvoker(), null);

        String name = "/" + endpoints[0];
        FortuneClient client = FortuneClientFactory.getFortuneClient(name);
        VContext ctxT = ctx.withTimeout(new Duration(20000)); // 20s
        assertThat(client.get(ctxT)).isEqualTo(TEST_INVOKER_FORTUNE);
    }

    public void testCustomDispatcherReturningAServer() throws Exception {
        Endpoint[] endpoints = s.listen(listenSpec);
        final FortuneServer server = new FortuneServerImpl();
        Dispatcher dispatcher = new Dispatcher() {
            @Override
            public ServiceObjectWithAuthorizer lookup(String suffix) throws VException {
                return new ServiceObjectWithAuthorizer(server, null);
            }
        };
        s.serveDispatcher("", dispatcher);

        String name = "/" + endpoints[0];
        FortuneClient client = FortuneClientFactory.getFortuneClient(name);
        VContext ctxT = ctx.withTimeout(new Duration(20000)); // 20s
        String firstMessage = "First fortune";
        client.add(ctxT, firstMessage);
        assertEquals(firstMessage, client.get(ctxT));
    }

    public void testCustomDispatcherReturningAnInvoker() throws Exception {
        Endpoint[] endpoints = s.listen(listenSpec);
        Dispatcher dispatcher = new Dispatcher() {
            @Override
            public ServiceObjectWithAuthorizer lookup(String suffix) throws VException {
                return new ServiceObjectWithAuthorizer(new TestInvoker(), null);
            }
        };
        s.serveDispatcher("", dispatcher);

        String name = "/" + endpoints[0];
        FortuneClient client = FortuneClientFactory.getFortuneClient(name);
        VContext ctxT = ctx.withTimeout(new Duration(20000)); // 20s
        assertThat(client.get(ctxT)).isEqualTo(TEST_INVOKER_FORTUNE);
    }

    private static class TestInvoker implements Invoker {
        @Override
        public Object[] invoke(VContext ctx, StreamServerCall call, String method, Object[] args)
                throws VException {
            if (call.security() == null) {
                throw new VException("Expected call.security() to return non-null");
            }
            if (call.remoteEndpoint() == null) {
                throw new VException("Expected remoteEndpoint() to return non-null");
            }
            if (method.equals("get")) {
                return new Object[] { TEST_INVOKER_FORTUNE };
            }
            throw new VException("Unsupported method: " + method);
        }
        @Override
        public Interface[] getSignature(VContext ctx, ServerCall call) throws VException {
            throw new VException("getSignature() unimplemented");
        }
        @Override
        public Method getMethodSignature(VContext ctx, ServerCall call, String method)
                throws VException {
            throw new VException("getMethodSignature() unimplemented");
        }
        @Override
        public Type[] getArgumentTypes(String method) throws VException {
            if (method.equals("get")) {
                return new Type[] {};
            }
            throw new VException("Unsupported method: " + method);
        }
        @Override
        public Type[] getResultTypes(String method) throws VException {
            if (method.equals("get")) {
                return new Type[] { String.class };
            }
            throw new VException("Unsupported method: " + method);
        }
        @Override
        public VdlValue[] getMethodTags(String method) throws VException {
            return new VdlValue[] {};
        }
        @Override
        public void glob(ServerCall call, String pattern, OutputChannel<GlobReply> responseChannel)
                throws VException {
            responseChannel.close();
        }
    }
}
