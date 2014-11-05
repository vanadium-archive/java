package io.veyron.jni.test.fortune;

import io.veyron.veyron.veyron2.OptionDefs;
import io.veyron.veyron.veyron2.Options;
import io.veyron.veyron.veyron2.context.Context;
import io.veyron.veyron.veyron2.ipc.Dispatcher;
import io.veyron.veyron.veyron2.ipc.Server;
import io.veyron.veyron.veyron2.ipc.ServerContext;
import io.veyron.veyron.veyron2.ipc.ServiceObjectWithAuthorizer;
import io.veyron.veyron.veyron2.ipc.VeyronException;
import io.veyron.veyron.veyron2.VRuntime;
import io.veyron.veyron.veyron2.RuntimeFactory;
import org.joda.time.Duration;

import android.test.AndroidTestCase;
import android.util.Log;

public class FortuneTest extends AndroidTestCase {
	private static VeyronException noneAdded = new VeyronException("no fortunes added");

	public static class FortuneServiceImpl implements FortuneService {
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

	}

    public void testFortuneJavaToJava()
        throws IllegalArgumentException, VeyronException {
    	// TODO(bprosnitz) We shouldn't have to initialize the default runtime to use non-default runtimes.
    	RuntimeFactory.initRuntime(getContext(), new Options());

    	final VRuntime serverRuntime = RuntimeFactory.newRuntime(getContext(), null);
    	final Server server = serverRuntime.newServer();
    	final String endpoint = server.listen(null);
    	final FortuneService service = new FortuneServiceImpl();
    	server.serve("fortune", new Dispatcher() {
            @Override
            public ServiceObjectWithAuthorizer lookup(String suffix)
                throws VeyronException {
            	return new ServiceObjectWithAuthorizer(service, null);
            }
        });
    	try {
	    	final Options options = new Options();
	    	options.set(OptionDefs.RUNTIME, serverRuntime);
	    	// TODO(bprosnitz) We get an ACL related error when using a different runtime than the server. Fix this.
	    	//Runtime clientRuntime = RuntimeFactory.newRuntime(getContext(),
	        //        null);
	    	//options.set(OptionDefs.RUNTIME_ID, serverRuntime.getIdentity());
	    	//options.set(OptionDefs.RUNTIME, clientRuntime);

	    	// TODO(bprosnitz) This gets around the mounttable by prefixing the endpoint by "/".
	    	// We should start the mounttable and use it for the test in the future.
	    	final String name = "/" + endpoint + "/fortune";

	    	final Fortune fortune = FortuneFactory.bind(name, options);

	    	final Context context = RuntimeFactory.defaultRuntime().newContext().withTimeout(
	    		new Duration(20000)); // 20s

	    	try {
	    		fortune.get(context);
	    		fail("Expected exception during call to get() before call to add()");
	    	} catch (VeyronException e) {
	    		assertEquals(e, noneAdded);
	    	}

	    	final String firstMessage = "First fortune";
	    	fortune.add(context, firstMessage);
	    	assertEquals(firstMessage, fortune.get(context));

	    	try {
	    		fortune.getSignature(context);
	    		fail("Expected Java server's signature method to return an error");
	    	} catch (VeyronException e) {}
    	} finally {
    		server.stop();
    	}
    }

}
