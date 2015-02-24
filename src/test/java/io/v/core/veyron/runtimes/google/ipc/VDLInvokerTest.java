package io.v.core.veyron.runtimes.google.ipc;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.reflect.TypeToken;

import android.test.AndroidTestCase;

import io.v.core.veyron2.android.V;
import io.v.core.veyron2.ipc.ServerCall;
import io.v.core.veyron2.ipc.ServerContext;
import io.v.core.veyron2.services.security.access.Constants;
import io.v.core.veyron2.vdl.Stream;
import io.v.core.veyron2.vdl.VdlUint32;
import io.v.core.veyron2.vdl.VdlValue;
import io.v.core.veyron2.verror.VException;
import io.v.core.veyron2.vom.VomUtil;
import io.v.jni.test.fortune.ComplexErrorParam;
import io.v.jni.test.fortune.Errors;
import io.v.jni.test.fortune.FortuneServer;

import java.io.EOFException;
import java.util.Arrays;
import java.util.Map;

public class VDLInvokerTest extends AndroidTestCase {
  static {
    V.init();
  }
  private static final ComplexErrorParam COMPLEX_PARAM = new ComplexErrorParam(
      "StrVal",
      11,
      ImmutableList.<VdlUint32>of(new VdlUint32(22), new VdlUint32(33)));

  private static final VException COMPLEX_ERROR = VException.explicitMake(
          Errors.ERR_COMPLEX, "en", "test", "test", COMPLEX_PARAM, "secondParam", 3);

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
        @Override
        public void getComplexError(ServerContext context) throws VException {
          throw COMPLEX_ERROR;
        }

        @Override
        public void noTags(ServerContext context) throws VException {}
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
            assertEquals(null, reply.vomAppError);
        }
        {
            final byte[][] args = new byte[][] {};
            final VDLInvoker.InvokeReply reply = invoker.invoke("get", call, args);
            assertEquals(1, reply.results.length);
            final String fortune = (String) VomUtil.decode(reply.results[0], String.class);
            assertEquals("test fortune", fortune);
            assertEquals(null, reply.vomAppError);
        }
        {
            // Test error.
            final byte[][] args = new byte[][] {};
            final VDLInvoker.InvokeReply reply = invoker.invoke("getComplexError", call, args);
            assertEquals(0, reply.results.length);
            if (reply.vomAppError == null) {
              fail("Expected error, got null");
            }
            final VException e = (VException) VomUtil.decode(reply.vomAppError, VException.class);
            if (!COMPLEX_ERROR.deepEquals(e)) {
                fail(String.format("Expected error %s, got %s", COMPLEX_ERROR, e));
            }
        }
    }

    public void testGetTags() throws VException {
        final VDLInvoker invoker = new VDLInvoker(new TestFortuneImpl());
        final Map<String, VdlValue[]> testCases = ImmutableMap.<String, VdlValue[]>builder()
                .put("add", new VdlValue[]{ Constants.WRITE })
                .put("get", new VdlValue[]{ Constants.READ })
                .put("streamingGet", new VdlValue[] { Constants.READ })
                .put("getComplexError", new VdlValue[] { Constants.READ })
                .put("noTags", new VdlValue[0])
                .build();
        for (final Map.Entry<String, VdlValue[]> testCase : testCases.entrySet()) {
            final String method = testCase.getKey();
            final VdlValue[] expected = testCase.getValue();
            final VdlValue[] actual = invoker.getMethodTags(method);
            if (!Arrays.equals(expected, actual)) {
                fail(String.format("Wrong tags for method %s, want %s, got %s", method,
                        Arrays.toString(expected), Arrays.toString(actual)));
            }
        }
    }
}
