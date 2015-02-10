package io.v.jni.test.fortune;

import android.test.AndroidTestCase;

import org.joda.time.Duration;

import io.v.core.veyron2.Options;
import io.v.core.veyron2.verror2.VException;
import io.v.core.veyron2.android.V;
import io.v.core.veyron2.context.VContext;
import io.v.core.veyron2.ipc.Server;
import io.v.core.veyron2.ipc.ServerContext;
import io.v.core.veyron2.vdl.ClientStream;
import io.v.core.veyron2.vdl.Stream;

import java.io.EOFException;

public class FortuneTest extends AndroidTestCase {
	public static class FortuneServerImpl implements FortuneServer {
		private String lastAddedFortune;

		@Override
		public String get(ServerContext context) throws VException {
			if (lastAddedFortune == null) {
				throw new VException("No fortunes added");
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
			// OK
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
}
