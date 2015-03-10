package io.v.v23.vdl;

import io.v.v23.vom.testdata.Constants;
import io.v.v23.vom.testdata.TestCase;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Tests that the VDL types are correctly parceled.
 */
public class SerializableTest extends junit.framework.TestCase {
    public void testSerializable() throws IOException, ClassNotFoundException {
        for (TestCase test : Constants.TESTS) {
            final Object value = test.getValue().getElem();
            if (!(value instanceof VdlValue)) continue;

            // Write
            final ByteArrayOutputStream data = new ByteArrayOutputStream();
            final ObjectOutputStream out = new ObjectOutputStream(data);
            out.writeObject(value);
            out.close();

            // Read
            final ObjectInputStream in =
                    new ObjectInputStream(new ByteArrayInputStream(data.toByteArray()));

            // Verify
            final Object copy = in.readObject();
            assertEquals(value, copy);
            assertEquals(value.hashCode(), copy.hashCode());
        }
    }
}
