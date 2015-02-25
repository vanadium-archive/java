package io.v.jni.test.fortune;

import com.google.common.collect.ImmutableList;

import android.test.AndroidTestCase;

import org.joda.time.Duration;

import io.v.v23.InputChannel;
import io.v.v23.V;
import io.v.v23.context.VContext;
import io.v.v23.ipc.NetworkChange;
import io.v.v23.ipc.Server;
import io.v.v23.ipc.ServerContext;
import io.v.v23.services.security.access.Constants;
import io.v.v23.vdl.ClientStream;
import io.v.v23.vdl.Stream;
import io.v.v23.vdl.VdlUint32;
import io.v.v23.vdl.VdlValue;
import io.v.v23.verror.VException;

import java.io.EOFException;
import java.util.Arrays;

public class FortuneTest extends AndroidTestCase {
    static {
        V.init();
    }
    private static final ComplexErrorParam COMPLEX_PARAM = new ComplexErrorParam(
            "StrVal",
            11,
            ImmutableList.<VdlUint32>of(new VdlUint32(22), new VdlUint32(33)));

    private static final VException COMPLEX_ERROR = VException.explicitMake(
            Errors.ERR_COMPLEX, "en", "test", "test", COMPLEX_PARAM, "secondParam", 3);
  
    public static class FortuneServerImpl implements FortuneServer {
        private String lastAddedFortune;

        @Override
        public String get(ServerContext context) throws VException {
            if (lastAddedFortune == null) {
                throw VException.make(Errors.ERR_NO_FORTUNES, context);
            }
            return lastAddedFortune;
        }

        @Override
        public void add(ServerContext context, String fortune) throws VException {
            lastAddedFortune = fortune;
        }

        @Override
        public int streamingGet(ServerContext context, Stream<String, Boolean> stream)
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
                    stream.send(get(context));
                } catch (VException e) {
                    throw new VException(
                            "Server couldn't send a string item: " + e.getMessage());
                }
                ++numSent;
            }
            return numSent;
        }

        @Override
        public void getComplexError(ServerContext context) throws VException {
            throw COMPLEX_ERROR;
        }

        @Override
        public void testContext(ServerContext context) throws VException {
            if (context == null) {
                throw new VException("Context is null");
            }
            if (context.timestamp() == null) {
                throw new VException("Timestamp is null");
            }
            if (!"testContext".equals(context.method())) {
                throw new VException(String.format("Wrong method, want \"testContext\", got %s",
                        context.method()));
            }
            final VdlValue[] expectedMethodTags = new VdlValue[]{ Constants.READ };
            if (!Arrays.equals(expectedMethodTags, context.methodTags())) {
                throw new VException(String.format("Wrong method tags, want %s, got %s",
                        expectedMethodTags, Arrays.toString(context.methodTags())));
            }
            if (context.suffix() == null) {
                throw new VException("Suffix is null");
            }
            if (context.localPrincipal() == null) {
                throw new VException("Local principal is null");
            }
            if (context.localBlessings() == null) {
                throw new VException("Local blessings are null");
            }
            if (context.remoteBlessings() == null) {
                throw new VException("Remote blessings are null");
            }
            if (context.localEndpoint() == null || context.localEndpoint().isEmpty()) {
                throw new VException("Local endpoint is empty");
            }
            if (context.remoteEndpoint() == null || context.remoteEndpoint().isEmpty()) {
                throw new VException("Remote endpoint is empty");
            }
            if (context.context() == null) {
                throw new VException("Vanadium context is null");
            }
        }

        @Override
        public void noTags(ServerContext context) throws VException {}
    }

    public void testFortune() throws VException {
        final VContext ctx = V.init();
        final Server s = V.newServer(ctx);
        final String[] endpoints = s.listen(null);
        final FortuneServer server = new FortuneServerImpl();
        s.serve("fortune", server);

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
        final String[] endpoints = s.listen(null);
        final FortuneServer server = new FortuneServerImpl();
        s.serve("fortune", server);

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
        final String[] endpoints = s.listen(null);
        final FortuneServer server = new FortuneServerImpl();
        s.serve("fortune", server);

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
        s.listen(null);
        final FortuneServer server = new FortuneServerImpl();
        s.serve("fortune", server);

        // TODO(spetrovic): Figure out how to force network change in android and test that the
        // changes get announced on this channel.
        final InputChannel<NetworkChange> channel = s.watchNetwork();
        s.unwatchNetwork(channel);
    }

    public void testContext() throws VException {
        final VContext ctx = V.init();
        final Server s = V.newServer(ctx);
        final String[] endpoints = s.listen(null);
        final FortuneServer server = new FortuneServerImpl();
        s.serve("fortune", server);

        final String name = "/" + endpoints[0];
        final FortuneClient client = FortuneClientFactory.bind(name);
        final VContext ctxT = ctx.withTimeout(new Duration(20000)); // 20s
        try {
            client.testContext(ctxT);
        } catch (VException e) {
            fail("Context check failed: " + e.getMessage());
        }
    }
}
