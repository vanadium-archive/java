package io.veyron.veyron.veyron2.vdl;

import com.google.common.collect.ImmutableList;

import junit.framework.TestCase;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

/**
 * Tests for VdlList class.
 */
public class VdlListTest extends TestCase {
    private static class ListListUint32 extends VdlList<List<VdlUint32>> {
        public ListListUint32(List<List<VdlUint32>> impl) {
            // TODO(rogulenko): get vdl type from reflection
            super(Types.listOf(Types.listOf(Types.UINT32)), impl);
        }
    }

    public void testSerialization() throws Exception {
        final ListListUint32 example = new ListListUint32(
                ImmutableList.<List<VdlUint32>>of(
                        ImmutableList.of(new VdlUint32(1))));
        assertEquals(1, example.get(0).get(0).getValue());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(example);
        oos.close();

        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));
        assertEquals(example, ois.readObject());
    }
}
