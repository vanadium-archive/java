package io.v.core.veyron.runtimes.google.ipc;

import com.google.common.reflect.TypeToken;

import junit.framework.TestCase;

import io.v.core.veyron2.vdl.Stream;

import java.io.EOFException;

import io.v.jni.test.fortune.FortuneServer;
import io.v.core.veyron2.verror2.VException;
import io.v.core.veyron2.util.VomUtil;
import io.v.core.veyron2.ipc.ServerCall;
import io.v.core.veyron2.ipc.ServerContext;

public class VDLInvokerTest extends TestCase {
    private static class TestFortuneImpl implements FortuneServer {
        String fortune = "";
        @Override
        public String get(ServerContext context) throws VException {
            return this.fortune;
        }
        @Override
        public void add(ServerContext context, String fortune) throws VException {
            this.fortune = fortune;
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
    }

    public void testInvoke() throws VException {
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
