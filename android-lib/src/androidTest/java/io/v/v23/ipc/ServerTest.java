package io.v.v23.ipc;

import android.test.AndroidTestCase;

import com.google.common.collect.Ordering;

import static com.google.common.truth.Truth.assertThat;

import io.v.v23.Options;
import io.v.v23.verror.VException;
import io.v.v23.android.V;
import io.v.v23.context.VContext;

import java.util.ArrayList;
import java.util.List;
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

    public void testAddRemoveName() throws Exception {
        final VContext ctx = V.init(getContext(), new Options());
        final Server s = V.newServer(ctx);
        s.listen(null);
        s.serve("name1", dummyDispatcher);
        s.addName("name2");
        assertThat(getNames(s)).containsExactly("name1", "name2");
        s.addName("name2");
        assertThat(getNames(s)).containsExactly("name1", "name2");
        s.addName("name3");
        assertThat(getNames(s)).containsExactly("name1", "name2", "name3");
        s.removeName("name2");
        assertThat(getNames(s)).containsExactly("name1", "name3");
        s.removeName("name2");
        assertThat(getNames(s)).containsExactly("name1", "name3");
    }

    private static List<String> getNames(Server s) {
        List<String> names = new ArrayList<String>();
        for (MountStatus mount : s.getStatus().getMounts()) {
            names.add(mount.getName());
        }
        return Ordering.natural().sortedCopy(names);
    }
}
