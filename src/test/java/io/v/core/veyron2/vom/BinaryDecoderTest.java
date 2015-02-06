package io.v.core.veyron2.vom;

import junit.framework.TestCase;

import io.v.core.veyron2.vdl.Types;
import io.v.core.veyron2.vdl.VdlArray;
import io.v.core.veyron2.vdl.VdlType;
import io.v.core.veyron2.vdl.VdlValue;
import io.v.core.veyron2.vom.testdata.Constants;

import java.util.Arrays;

public class BinaryDecoderTest extends TestCase {
    private void assertEqual(Object expected, Object actual) {
        if (expected.getClass().isArray()) {
            Class<?> component = expected.getClass().getComponentType();
            if (component == Boolean.TYPE) {
                assertTrue(Arrays.equals((boolean[]) expected, (boolean[]) actual));
            } else if (component == Byte.TYPE) {
                assertTrue(Arrays.equals((byte[]) expected, (byte[]) actual));
            } else if (component == Short.TYPE) {
                assertTrue(Arrays.equals((short[]) expected, (short[]) actual));
            } else if (component == Integer.TYPE) {
                assertTrue(Arrays.equals((int[]) expected, (int[]) actual));
            } else if (component == Long.TYPE) {
                assertTrue(Arrays.equals((long[]) expected, (long[]) actual));
            } else if (component == Float.TYPE) {
                assertTrue(Arrays.equals((float[]) expected, (float[]) actual));
            } else if (component == Double.TYPE) {
                assertTrue(Arrays.equals((double[]) expected, (double[]) actual));
            } else {
                assertTrue(Arrays.equals((Object[]) expected, (Object[]) actual));
            }
        } else {
            assertEquals(expected, actual);
        }
    }

    public void testDecode() throws Exception {
        for (io.v.core.veyron2.vom.testdata.TestCase test : Constants.TESTS) {
            byte[] bytes = TestUtil.hexStringToBytes(test.getHex());
            Object value;
            if (test.getValue().getElem().getClass().isArray()) {
                value = TestUtil.decode(bytes, test.getValue().getElem().getClass());
            } else {
                value = TestUtil.decode(bytes);
            }
            assertEqual(test.getValue().getElem(), value);
        }
    }

    public void testDecodeEncode() throws Exception {
        for (io.v.core.veyron2.vom.testdata.TestCase test : Constants.TESTS) {
            byte[] bytes = TestUtil.hexStringToBytes(test.getHex());
            VdlValue value = (VdlValue) TestUtil.decode(bytes, VdlValue.class);
            assertEquals(test.getHex(), TestUtil.encode(value.vdlType(), value));
        }

        VdlType testsType = Types.getVdlTypeFromReflect(
                Constants.class.getDeclaredField("TESTS").getGenericType());
        String encoded = TestUtil.encode(testsType, Constants.TESTS);
        VdlValue decoded = (VdlValue) TestUtil.decode(
                TestUtil.hexStringToBytes(encoded));
        assertEquals(encoded, TestUtil.encode(decoded.vdlType(), decoded));
    }

    public void testDecodeVdlArray() throws Exception {
        VdlArray<?> v = new VdlArray<Byte>(Types.arrayOf(4, Types.BYTE), new Byte[]{1, 2, 3, 4});
        byte[] encoded = TestUtil.hexStringToBytes(TestUtil.encode(v));
        Object decoded = TestUtil.decode(encoded);
        assertNotNull(decoded);
    }
}
