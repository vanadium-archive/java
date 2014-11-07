package io.veyron.veyron.veyron2.vom2;

import junit.framework.TestCase;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * Tests that the low-level VOM writer and writer are compatible with expected
 * VOM binary.
 */
public class RawVomCompatibilityTest extends TestCase {
    private static class TestData {
        public char type;
        public Object obj;
        public String hexString;

        public TestData(char type, Object obj, String hexString) {
            this.type = type;
            this.obj = obj;
            this.hexString = hexString;
        }
    }

    private static TestData[] tests = new TestData[] {
            new TestData('B', false, "00"),
            new TestData('B', true, "01"),

            new TestData('U', 0L, "00"),
            new TestData('U', 1L, "01"),
            new TestData('U', 2L, "02"),
            new TestData('U', 127L, "7f"),
            new TestData('U', 128L, "ff80"),
            new TestData('U', 255L, "ffff"),
            new TestData('U', 256L, "fe0100"),
            new TestData('U', 257L, "fe0101"),
            new TestData('U', 0xffffL, "feffff"),
            new TestData('U', 0xffffffL, "fdffffff"),
            new TestData('U', 0xffffffffL, "fcffffffff"),
            new TestData('U', 0xffffffffffL, "fbffffffffff"),
            new TestData('U', 0xffffffffffffL, "faffffffffffff"),
            new TestData('U', 0xffffffffffffffL, "f9ffffffffffffff"),
            new TestData('U', 0xffffffffffffffffL, "f8ffffffffffffffff"),

            new TestData('I', 0L, "00"),
            new TestData('I', 1L, "02"),
            new TestData('I', 2L, "04"),
            new TestData('I', 63L, "7e"),
            new TestData('I', 64L, "ff80"),
            new TestData('I', 65L, "ff82"),
            new TestData('I', 127L, "fffe"),
            new TestData('I', 128L, "fe0100"),
            new TestData('I', 129L, "fe0102"),
            new TestData('I', Long.valueOf(Short.MAX_VALUE), "fefffe"),
            new TestData('I', Long.valueOf(Integer.MAX_VALUE), "fcfffffffe"),
            new TestData('I', Long.MAX_VALUE, "f8fffffffffffffffe"),

            new TestData('I', -1L, "01"),
            new TestData('I', -2L, "03"),
            new TestData('I', -64L, "7f"),
            new TestData('I', -65L, "ff81"),
            new TestData('I', -66L, "ff83"),
            new TestData('I', -128L, "ffff"),
            new TestData('I', -129L, "fe0101"),
            new TestData('I', -130L, "fe0103"),
            new TestData('I', Long.valueOf(Short.MIN_VALUE), "feffff"),
            new TestData('I', Long.valueOf(Integer.MIN_VALUE), "fcffffffff"),
            new TestData('I', Long.MIN_VALUE, "f8ffffffffffffffff"),

            new TestData('F', 0.0, "00"),
            new TestData('F', 1.0, "fef03f"),
            new TestData('F', 17.0, "fe3140"),
            new TestData('F', 18.0, "fe3240"),

            new TestData('S', "", "00"),
            new TestData('S', "abc", "03616263"),
            new TestData('S', "defghi", "06646566676869"),
    };

    public void testRawWriterCompatibility() throws IOException {
        for (TestData test : tests) {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            RawVomWriter w = new RawVomWriter(os);

            switch (test.type) {
                case 'B':
                    w.writeBoolean((Boolean) test.obj);
                    break;
                case 'U':
                    w.writeUint((Long) test.obj);
                    break;
                case 'I':
                    w.writeInt((Long) test.obj);
                    break;
                case 'F':
                    w.writeFloat((Double) test.obj);
                    break;
                case 'S':
                    w.writeString((String) test.obj);
                    break;
                default:
                    throw new RuntimeException("Unexpected type " + test.type);
            }

            byte[] expected = TestUtil.hexStringToBytes(test.hexString);
            byte[] actual = os.toByteArray();
            assertTrue(
                    "Failed while testing " + test.obj + " got "
                            + TestUtil.bytesToHexString(actual) + " but expected "
                            + TestUtil.bytesToHexString(expected), Arrays.equals(expected, actual));
        }
    }

    public void testRawReaderCompatibility() throws IOException {
        for (TestData test : tests) {
            byte[] inputData = TestUtil.hexStringToBytes(test.hexString);
            ByteArrayInputStream is = new ByteArrayInputStream(inputData);
            RawVomReader r = new RawVomReader(is);

            Object readObj;
            switch (test.type) {
                case 'B':
                    readObj = r.readBoolean();
                    break;
                case 'U':
                    readObj = r.readUint();
                    break;
                case 'I':
                    readObj = r.readInt();
                    break;
                case 'F':
                    readObj = r.readFloat();
                    break;
                case 'S':
                    readObj = r.readString();
                    break;
                default:
                    throw new RuntimeException("Unexpected type " + test.type);
            }

            assertEquals("Failed while testing " + test.hexString, test.obj, readObj);
        }
    }
}
