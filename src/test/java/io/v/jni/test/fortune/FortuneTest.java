package io.v.jni.test.fortune;

import android.test.AndroidTestCase;
import android.util.Log;

import org.joda.time.Duration;

import io.v.core.veyron2.vdl.ClientStream;
import io.v.core.veyron2.Options;
import io.v.core.veyron2.VeyronException;
import io.v.core.veyron2.android.VRuntime;
import io.v.core.veyron2.context.Context;
import io.v.core.veyron2.ipc.Server;
import io.v.core.veyron2.ipc.ServerContext;
import io.v.core.veyron2.vdl.Stream;

import java.io.EOFException;
import java.util.Arrays;

public class FortuneTest extends AndroidTestCase {
	public static class FortuneServerImpl implements FortuneServer {
		private String lastAddedFortune;

		@Override
		public String get(ServerContext context) throws VeyronException {
			if (lastAddedFortune == null) {
				throw new VeyronException("No fortunes added");
			}
			return lastAddedFortune;
		}

		@Override
		public void add(ServerContext context, String fortune) throws VeyronException {
			lastAddedFortune = fortune;
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

	public void testFortune() throws VeyronException {
		VRuntime.init(getContext(), new Options());
		final Server s = VRuntime.newServer();
		final String[] endpoints = s.listen(null);
		final FortuneServer server = new FortuneServerImpl();
		s.serve("fortune", server);

		final String name = "/" + endpoints[0];
		final FortuneClient client = FortuneClientFactory.bind(name);
		final Context context = VRuntime.newContext().withTimeout(new Duration(20000)); // 20s
		try {
			client.get(context);
			fail("Expected exception during call to get() before call to add()");
		} catch (VeyronException e) {
			// OK
		}
		final String firstMessage = "First fortune";
		client.add(context, firstMessage);
		assertEquals(firstMessage, client.get(context));
		s.stop();
	}
   
	public void testStreaming() throws VeyronException {
		VRuntime.init(getContext(), new Options());
		final Server s = VRuntime.newServer();
		final String[] endpoints = s.listen(null);
		final FortuneServer server = new FortuneServerImpl();
		s.serve("fortune", server);

		final String name = "/" + endpoints[0];
		final FortuneClient client = FortuneClientFactory.bind(name);
		final Context context = VRuntime.newContext().withTimeout(new Duration(20000));  // 20s
		final ClientStream<Boolean, String, Integer> stream = client.streamingGet(context);
		final String msg = "The only fortune";
		client.add(context, msg);
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
