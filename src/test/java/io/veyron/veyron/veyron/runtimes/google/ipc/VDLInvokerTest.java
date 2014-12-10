package io.veyron.veyron.veyron.runtimes.google.ipc;

import com.google.common.reflect.TypeToken;

import junit.framework.TestCase;

import io.veyron.veyron.veyron2.vdl.Stream;

import java.io.EOFException;

import io.veyron.jni.test.fortune.FortuneServer;
import io.veyron.veyron.veyron2.VeyronException;
import io.veyron.veyron.veyron2.VomUtil;
import io.veyron.veyron.veyron2.ipc.ServerCall;
import io.veyron.veyron.veyron2.ipc.ServerContext;

public class VDLInvokerTest extends TestCase {
    private static class TestFortuneImpl implements FortuneServer {
        String fortune = "";
        @Override
        public String get(ServerContext context) throws VeyronException {
            return this.fortune;
        }
        @Override
        public void add(ServerContext context, String fortune) throws VeyronException {
            this.fortune = fortune;
        }
        @Override
        public int streamingGet(ServerContext context, Stream<String, Boolean> stream)
                throws VeyronException {
            int numSent = 0;
            while (true) {
                try {
                    stream.recv();
                } catch (VeyronException e) {
                    throw new VeyronException(
                          "Server couldn't receive a boolean item: " + e.getMessage());
                } catch (EOFException e) {
                    break;
                }
                try {
                    stream.send(get(context));
                } catch (VeyronException e) {
                    throw new VeyronException(
                            "Server couldn't send a string item: " + e.getMessage());
                }
                ++numSent;
            }
            return numSent;
        }
    }

    public void testGetMethodTags() throws IllegalArgumentException, VeyronException {
        final VDLInvoker invoker = new VDLInvoker(new TestFortuneImpl());
        final Object[] tags = invoker.getMethodTags("get");
        assertEquals(1, tags.length);
        assertEquals(io.veyron.veyron.veyron2.security.SecurityConstants.READ_LABEL, tags[0]);
    }

    public void testInvoke() throws VeyronException {
        final VDLInvoker invoker = new VDLInvoker(new TestFortuneImpl());
        final ServerCall call = null;
        {
            final byte[][] args = new byte[][] {
                    VomUtil.encode("test fortune", new TypeToken<String>(){}.getType())
            };
            final VDLInvoker.InvokeReply reply = invoker.invoke("add", call, args);
            assertEquals(0, reply.results.length);
            assertEquals(false, reply.hasApplicationError);
            assertEquals(null, reply.errorID);
            assertEquals(null, reply.errorMsg);
        }
        {
            final byte[][] args = new byte[][] {};
            final VDLInvoker.InvokeReply reply = invoker.invoke("get", call, args);
            assertEquals(1, reply.results.length);
            assertEquals("\"test fortune\"", reply.results[0]);
            assertEquals(false, reply.hasApplicationError);
            assertEquals(null, reply.errorID);
            assertEquals(null, reply.errorMsg);
        }
    }
}
