package io.v.jni.test.fortune;

import com.google.common.collect.ImmutableList;

import android.test.AndroidTestCase;

import org.joda.time.Duration;

import io.v.core.veyron2.Options;
import io.v.core.veyron2.android.V;
import io.v.core.veyron2.context.VContext;
import io.v.core.veyron2.ipc.Server;
import io.v.core.veyron2.ipc.ServerContext;
import io.v.core.veyron2.vdl.ClientStream;
import io.v.core.veyron2.vdl.Stream;
import io.v.core.veyron2.vdl.VdlUint32;
import io.v.core.veyron2.verror.VException;

import java.io.EOFException;
import java.io.Serializable;
import java.lang.reflect.Type;

public class FortuneTest extends AndroidTestCase {
    private static final ComplexErrorParam COMPLEX_PARAM = new ComplexErrorParam(
            "StrVal",
            11,
            ImmutableList.<VdlUint32>of(new VdlUint32(22), new VdlUint32(33)));

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
            throw VException.make(Errors.ERR_COMPLEX, context, COMPLEX_PARAM, "secondParam", 3);
        }
    }

    public void testFortune() throws VException {
        final VContext ctx = V.init(getContext(), new Options());
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
        final VContext ctx = V.init(getContext(), new Options());
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
        final VContext ctx = V.init(getContext(), new Options());
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
            if (!e.is(Errors.ERR_COMPLEX)) {
                fail(String.format("Expected error %s, got %s", Errors.ERR_COMPLEX, e));
            }
            final Serializable[] params = e.getParams();
            final Type[] paramTypes = e.getParamTypes();
            assertEquals(5, params.length);
            assertEquals(5, paramTypes.length);
            assertEquals(String.class, paramTypes[0]);  // componentName
            assertEquals(String.class, paramTypes[1]);  // opName
            assertEquals(COMPLEX_PARAM, params[2]);
            assertEquals(ComplexErrorParam.class, paramTypes[2]);
            assertEquals("secondParam", params[3]);
            assertEquals(String.class, paramTypes[3]);
            assertEquals(3, params[4]);
            assertEquals(Integer.class, paramTypes[4]);
        }
        s.stop();
    }
}
