package io.v.core.veyron2.ipc;

import android.test.AndroidTestCase;

import io.v.core.veyron2.Options;
import io.v.core.veyron2.verror.VException;
import io.v.core.veyron2.android.V;
import io.v.core.veyron2.context.VContext;

import java.util.Arrays;
import java.util.SortedSet;
import java.util.TreeSet;

public class ServerTest extends AndroidTestCase {
	private final Dispatcher dummyDispatcher;

	public ServerTest() {
		dummyDispatcher = new Dispatcher() {
			@Override
			public ServiceObjectWithAuthorizer lookup(String suffix) throws VException {
				throw new VException("Lookup unimplemented.");
			}
		};
	}

	public void testAddRemoveName() {
		final VContext ctx = V.init(getContext(), new Options());
		try {
			final Server s = V.newServer(ctx);
			s.serve("name1", dummyDispatcher);
			s.addName("name2");
			assertTrue(Arrays.equals(new String[]{ "name1", "name2" }, getNames(s)));
			s.addName("name2");
			assertTrue(Arrays.equals(new String[]{ "name1", "name2" }, getNames(s)));
			s.addName("name3");
			assertTrue(Arrays.equals(new String[]{ "name1", "name2", "name3" }, getNames(s)));
			s.removeName("name2");
			assertTrue(Arrays.equals(new String[]{ "name1", "name3" }, getNames(s)));
			s.removeName("name2");
			assertTrue(Arrays.equals(new String[]{ "name1", "name3" }, getNames(s)));
		} catch (VException e) {
			fail("Unexpected exception: " + e.getMessage());
		}
	}

	private static String[] getNames(Server s) {
		final MountStatus[] mounts = s.getStatus().getMounts();
		final SortedSet<String> names = new TreeSet<String>(); 
		for (int i = 0; i < mounts.length; ++i) {
			names.add(mounts[i].getName());
		}
		return names.toArray(new String[0]);
	}
}
