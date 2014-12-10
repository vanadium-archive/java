package io.veyron.jni.test.fortune;

import io.veyron.veyron.veyron2.Options;
import io.veyron.veyron.veyron2.context.Context;
import io.veyron.veyron.veyron2.ipc.Server;
import io.veyron.veyron.veyron2.ipc.ServerContext;
import io.veyron.veyron.veyron2.VeyronException;
import io.veyron.veyron.veyron2.android.VRuntime;

import org.joda.time.Duration;

import io.veyron.veyron.veyron2.vdl.Stream;

import java.io.EOFException;

import android.test.AndroidTestCase;

public class FortuneTest extends AndroidTestCase {
	private static VeyronException noneAdded = new VeyronException("no fortunes added");

	public static class FortuneServerImpl implements FortuneServer {
		private String lastAddedFortune;

		@Override
		public String get(ServerContext context) throws VeyronException {
			if (lastAddedFortune == null) {
				throw noneAdded;
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

    public void testFortuneJavaToJava() throws VeyronException {
    	VRuntime.init(getContext(), new Options());
    	final Server s = VRuntime.newServer();
    	final String endpoint = s.listen(null);
    	final FortuneServer server = new FortuneServerImpl();
    	s.serve("fortune", server);
    	try {
	    	final String name = "/" + endpoint + "/fortune";
	    	final FortuneClient client = FortuneClientFactory.bind(name);
	    	final Context context = VRuntime.newContext().withTimeout(new Duration(20000)); // 20s
	    	try {
	    		client.get(context);
	    		fail("Expected exception during call to get() before call to add()");
	    	} catch (VeyronException e) {
	    		assertEquals(e, noneAdded);
	    	}
	    	final String firstMessage = "First fortune";
	    	client.add(context, firstMessage);
	    	assertEquals(firstMessage, client.get(context));
	    	try {
	    		client.getSignature(context);
	    		fail("Expected Java server's signature method to return an error");
	    	} catch (VeyronException e) {}
    	} finally {
    		s.stop();
    	}
    }

}
