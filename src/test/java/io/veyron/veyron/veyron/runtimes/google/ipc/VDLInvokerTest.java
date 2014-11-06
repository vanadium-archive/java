
package io.veyron.veyron.veyron.runtimes.google.ipc;

import java.util.Arrays;

import junit.framework.TestCase;

import io.veyron.veyron.veyron.testing.TestUtil;
import io.veyron.veyron.veyron2.ipc.ServerCall;
import io.veyron.veyron.veyron2.ipc.ServerContext;
import io.veyron.veyron.veyron2.VeyronException;
import io.veyron.veyron.veyron2.security.Label;
import io.veyron.veyron.veyron2.vdl.Stream;
import io.veyron.jni.test.fortune.FortuneService;

public class VDLInvokerTest extends TestCase {
    private static class TestFortuneImpl implements FortuneService {
        String fortune = "";
        @Override
        public String get(ServerContext context) throws VeyronException {
            return this.fortune;
        }
        @Override
        public void add(ServerContext context, String fortune) throws VeyronException {
            this.fortune = fortune;
        }
    }

    public void testGetImplementedServices() throws IllegalArgumentException, VeyronException {
        final String[] expectedImplementedServices = new String[] {
                "veyron.io/jni/test/fortune/FortuneService"
        };
        final VDLInvoker invoker = new VDLInvoker(new TestFortuneImpl());
        final String[] implementedServices = invoker.getImplementedServices();
        Arrays.sort(implementedServices);
        TestUtil.assertArrayEquals(expectedImplementedServices, implementedServices);
    }

    public void testGetMethodTags() throws IllegalArgumentException, VeyronException {
        final VDLInvoker invoker = new VDLInvoker(new TestFortuneImpl());
        final Object[] tags = invoker.getMethodTags("get");
        assertEquals(1, tags.length);
        assertEquals(io.veyron.veyron.veyron2.security.SecurityConstants.READ_LABEL, tags[0]);
    }

    public void testInvoke()
        throws VeyronException, IllegalArgumentException, IllegalAccessException {
        final VDLInvoker invoker = new VDLInvoker(new TestFortuneImpl());
        final ServerCall call = null;
        {
            final String[] args = new String[] { "\"test fortune\"" };
            final VDLInvoker.InvokeReply reply = invoker.invoke("add", call, args);
            assertEquals(0, reply.results.length);
            assertEquals(false, reply.hasApplicationError);
            assertEquals(null, reply.errorID);
            assertEquals(null, reply.errorMsg);
        }
        {
            final String[] args = new String[] {};
            final VDLInvoker.InvokeReply reply = invoker.invoke("get", call, args);
            assertEquals(1, reply.results.length);
            assertEquals("\"test fortune\"", reply.results[0]);
            assertEquals(false, reply.hasApplicationError);
            assertEquals(null, reply.errorID);
            assertEquals(null, reply.errorMsg);
        }
    }
}
