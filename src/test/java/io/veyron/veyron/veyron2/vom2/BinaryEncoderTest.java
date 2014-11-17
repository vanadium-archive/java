package io.veyron.veyron.veyron2.vom2;

import junit.framework.TestCase;

import io.veyron.veyron.veyron2.vdl.Types;

import io.veyron.veyron.veyron2.vdl.VdlType;
import io.veyron.veyron.veyron2.vdl.VdlAny;
import io.veyron.veyron.veyron2.vom2.testdata.TestdataConstants;

import java.io.ByteArrayOutputStream;

public class BinaryEncoderTest extends TestCase {
    private static String encode(VdlType type, Object value) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        BinaryEncoder encoder = new BinaryEncoder(out);
        encoder.encodeValue(type, value);
        return TestUtil.bytesToHexString(out.toByteArray());
    }

    public void testEncode() throws Exception {
        for (io.veyron.veyron.veyron2.vom2.testdata.TestCase test : TestdataConstants.TESTS) {
            // TODO(rogulenko): remove this after disallowing unnamed arrays
            if (test.getName().contains("[2]")) {
                continue;
            }
            VdlAny value = test.getValue();
            assertEquals(test.getHex(), encode(value.getElemType(), value.getElem()));
        }

        // TODO(rogulenko): ensure compatibility with go after vdl.Any issue is solved
        VdlType testsType = Types.getVdlTypeFromReflection(
                TestdataConstants.class.getDeclaredField("TESTS").getGenericType());
        assertNotNull(encode(testsType, TestdataConstants.TESTS));
    }
}
