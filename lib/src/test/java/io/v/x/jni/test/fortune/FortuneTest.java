// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.x.jni.test.fortune;

import com.google.common.collect.ImmutableList;
import com.google.common.reflect.TypeToken;
import com.google.common.util.concurrent.SettableFuture;
import com.google.common.util.concurrent.Uninterruptibles;

import junit.framework.TestCase;

import org.joda.time.Duration;

import java.io.EOFException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import io.v.v23.OutputChannel;
import io.v.v23.V;
import io.v.v23.context.CancelableVContext;
import io.v.v23.context.VContext;
import io.v.v23.naming.GlobReply;
import io.v.v23.rpc.Callback;
import io.v.v23.rpc.Client;
import io.v.v23.rpc.ClientCall;
import io.v.v23.rpc.Dispatcher;
import io.v.v23.rpc.Invoker;
import io.v.v23.rpc.ListenSpec;
import io.v.v23.rpc.Server;
import io.v.v23.rpc.ServerCall;
import io.v.v23.rpc.ServiceObjectWithAuthorizer;
import io.v.v23.rpc.StreamServerCall;
import io.v.v23.vdl.TypedClientStream;
import io.v.v23.vdl.VdlValue;
import io.v.v23.vdlroot.signature.Interface;
import io.v.v23.vdlroot.signature.Method;
import io.v.v23.verror.CanceledException;
import io.v.v23.verror.VException;

import static com.google.common.truth.Truth.assertThat;

public class FortuneTest extends TestCase {
    private static final String TEST_INVOKER_FORTUNE = "Test invoker fortune";

    private VContext ctx;

    @Override
    protected void setUp() throws Exception {
        ctx = V.init();
        ListenSpec.Address addr = new ListenSpec.Address("tcp", "127.0.0.1:0");
        ctx = V.withListenSpec(ctx, V.getListenSpec(ctx).withAddress(addr));
    }

    @Override
    protected void tearDown() throws Exception {
        Server s = V.getServer(ctx);
        if (s != null) {
            s.stop();
        }
        V.shutdown();
    }

    private String name() {
        Server s = V.getServer(ctx);
        if (s == null) {
            return "";
        }
        return "/" + s.getStatus().getEndpoints()[0];
    }

    public void testFortune() throws Exception {
        FortuneServer server = new FortuneServerImpl();
        ctx = V.withNewServer(ctx, "", server, null);

        FortuneClient client = FortuneClientFactory.getFortuneClient(name());
        VContext ctxT = ctx.withTimeout(new Duration(20000)); // 20s
        try {
            client.get(ctxT);
            fail("Expected exception during call to get() before call to add()");
        } catch (NoFortunesException e) {
            // OK
        } catch (VException e) {
            fail("Expected NoFortuneException, got: " + e);
        }
        String firstMessage = "First fortune";
        client.add(ctxT, firstMessage);
        assertEquals(firstMessage, client.get(ctxT));
    }

    public void testAsyncFortune() throws Exception {
        FortuneServer server = new FortuneServerImpl();
        ctx = V.withNewServer(ctx, "", server, null);

        FortuneClient client = FortuneClientFactory.getFortuneClient(name());
        VContext ctxT = ctx.withTimeout(new Duration(2000000)); // 20s
        client.add(ctxT, "Hello world");
        FutureCallback<String> result = new FutureCallback<>();
        client.get(ctxT, result);
        assertThat(Uninterruptibles.getUninterruptibly(
                result.getFuture(), 5, TimeUnit.SECONDS)).isNotEmpty();
    }

    public void testAsyncFortuneWithCancel() throws Exception {
        CountDownLatch callLatch = new CountDownLatch(1);
        FortuneServer server = new FortuneServerImpl(callLatch);
        ctx = V.withNewServer(ctx, "", server, null);
        CancelableVContext cancelCtx = ctx.withCancel();

        FortuneClient client = FortuneClientFactory.getFortuneClient(name());
        VContext ctxT = ctx.withTimeout(new Duration(20000)); // 20s
        client.add(ctxT, "Hello world");
        FutureCallback<String> result = new FutureCallback<>();
        client.get(cancelCtx, result);
        // Cancel the RPC.
        cancelCtx.cancel();
        // Allow the server RPC impl to finish.
        callLatch.countDown();
        // The call should have failed, it was canceled before it completed.
        try {
            result.getFuture().get();
            fail("Should have failed!");
        } catch (ExecutionException e) {
            assertThat(e.getCause()).isInstanceOf(CanceledException.class);
        }
    }

    public void testStreaming() throws Exception {
        FortuneServer server = new FortuneServerImpl();
        ctx = V.withNewServer(ctx, "", server, null);

        FortuneClient client = FortuneClientFactory.getFortuneClient(name());
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

    public void testAsyncStreaming() throws Throwable {
        FortuneServer server = new FortuneServerImpl();
        ctx = V.withNewServer(ctx, "", server, null);

        FortuneClient client = FortuneClientFactory.getFortuneClient(name());
        VContext ctxT = ctx.withTimeout(new Duration(20000));  // 20s
        final String msg = "The only fortune";
        client.add(ctxT, msg);
        FutureCallback<TypedClientStream<Boolean, String, Integer>> callback
                = new FutureCallback<>();
        client.streamingGet(ctxT, callback);
        TypedClientStream<Boolean, String, Integer> stream
                = Uninterruptibles.getUninterruptibly(callback.getFuture(), 1, TimeUnit.SECONDS);
        for (int i = 0; i < 5; i++) {
            stream.send(true);
            assertEquals(msg, stream.recv());
        }
        int total = stream.finish();
        assertEquals(5, total);
    }

    public void testMultiple() throws Exception {
        FortuneServer server = new FortuneServerImpl();
        ctx = V.withNewServer(ctx, "", server, null);

        FortuneClient client = FortuneClientFactory.getFortuneClient(name());
        VContext ctxT = ctx.withTimeout(new Duration(20000)); // 20s
        String firstMessage = "First fortune";
        client.add(ctxT, firstMessage);

        FortuneClient.MultipleGetOut ret = client.multipleGet(ctxT);
        assertEquals(firstMessage, ret.fortune);
        assertEquals(firstMessage, ret.another);
    }

    public void testComplexError() throws Exception {
        FortuneServer server = new FortuneServerImpl();
        ctx = V.withNewServer(ctx, "", server, null);

        FortuneClient client = FortuneClientFactory.getFortuneClient(name());
        VContext ctxT = ctx.withTimeout(new Duration(20000)); // 20s
        try {
            client.getComplexError(ctxT);
            fail("Expected exception during call to getComplexError()");
        } catch (VException e) {
            if (!FortuneServerImpl.COMPLEX_ERROR.deepEquals(e)) {
                fail(String.format("Expected error %s, got %s",
                        FortuneServerImpl.COMPLEX_ERROR, e));
            }
        }
    }

    public void testContext() throws Exception {
        FortuneServer server = new FortuneServerImpl();
        ctx = V.withNewServer(ctx, "", server, null);

        FortuneClient client = FortuneClientFactory.getFortuneClient(name());
        VContext ctxT = ctx.withTimeout(new Duration(20000)); // 20s
        try {
            client.testServerCall(ctxT);
        } catch (VException e) {
            fail("Context check failed: " + e.getMessage());
        }
    }

    public void testGetServer() throws Exception {
        FortuneServer server = new FortuneServerImpl();
        ctx = V.withNewServer(ctx, "", server, null);
        Server s = V.getServer(ctx);
        assertThat(s).isNotNull();
    }

    public void testGetSignature() throws Exception {
        FortuneServer server = new FortuneServerImpl();
        ctx = V.withNewServer(ctx, "", server, null);

        Client c = V.getClient(ctx);
        VContext ctxT = ctx.withTimeout(new Duration(20000)); // 20s
        ClientCall call = c.startCall(ctxT, name(), "__Signature", new Object[0], new Type[0]);
        Object[] results = call.finish(new Type[] { new TypeToken<Interface[]>() {}.getType() });
        assertThat(results.length == 1).isTrue();
        Interface[] signature = (Interface[]) results[0];
        assertThat(signature.length >= 1).isTrue();
        assertThat(signature[0].getMethods()).isNotEmpty();
    }

    public void testGlob() throws Exception {
        FortuneServer server = new FortuneServerImpl();
        ctx = V.withNewServer(ctx, "", server, null);

        List<GlobReply> globResult
                = ImmutableList.copyOf(V.getNamespace(ctx).glob(ctx, name() + "/*"));
        assertThat(globResult).hasSize(2);
        assertThat(globResult.get(0)).isInstanceOf(GlobReply.Entry.class);
        assertThat(((GlobReply.Entry) globResult.get(0)).getElem().getName())
                .isEqualTo(name() + "/helloworld");
        assertThat(globResult.get(1)).isInstanceOf(GlobReply.Error.class);
    }

    public void testCustomInvoker() throws Exception {
        ctx = V.withNewServer(ctx, "", new TestInvoker(), null);

        FortuneClient client = FortuneClientFactory.getFortuneClient(name());
        VContext ctxT = ctx.withTimeout(new Duration(20000)); // 20s
        assertThat(client.get(ctxT)).isEqualTo(TEST_INVOKER_FORTUNE);
    }

    public void testCustomDispatcherReturningAServer() throws Exception {
        final FortuneServer server = new FortuneServerImpl();
        Dispatcher dispatcher = new Dispatcher() {
            @Override
            public ServiceObjectWithAuthorizer lookup(String suffix) throws VException {
                return new ServiceObjectWithAuthorizer(server, null);
            }
        };
        ctx = V.withNewServer(ctx, "", dispatcher);

        FortuneClient client = FortuneClientFactory.getFortuneClient(name());
        VContext ctxT = ctx.withTimeout(new Duration(20000)); // 20s
        String firstMessage = "First fortune";
        client.add(ctxT, firstMessage);
        assertEquals(firstMessage, client.get(ctxT));
    }

    public void testCustomDispatcherReturningAnInvoker() throws Exception {
        Dispatcher dispatcher = new Dispatcher() {
            @Override
            public ServiceObjectWithAuthorizer lookup(String suffix) throws VException {
                return new ServiceObjectWithAuthorizer(new TestInvoker(), null);
            }
        };
        ctx = V.withNewServer(ctx, "", dispatcher);

        FortuneClient client = FortuneClientFactory.getFortuneClient(name());
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

    private static class FutureCallback<T> implements Callback<T> {
        private final SettableFuture<T> future = SettableFuture.create();

        public Future<T> getFuture() {
            return future;
        }

        @Override
        public void onSuccess(T result) {
            future.set(result);
        }

        @Override
        public void onFailure(VException error) {
            future.setException(error);
        }
    }

}
