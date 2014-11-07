package io.veyron.veyron.veyron2.vom2;

import junit.framework.TestCase;

import io.veyron.veyron.veyron.testing.BufferedPipedInputStream;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Tests that low-level VOM writes can be read and recovered.
 */
public class RawVomRecoverabilityTest extends TestCase {
    private RawVomReader reader;
    private RawVomWriter writer;

    private OutputStream os;

    @Override
    public void setUp() throws IOException {
        BufferedPipedInputStream is = new BufferedPipedInputStream();
        os = is.getOutputStream();
        writer = new RawVomWriter(os);
        reader = new RawVomReader(is);
    }

    public void testEofUint() throws IOException {
        os.write(0xfb); // 6-bytes
        os.close();
        try {
            reader.readUint();
            fail("Expected EOF. Not enough bytes written to stream.");
        } catch (CorruptVomStreamException e) {
            // expected.
        }
    }

    public void testEofBoolean() throws IOException {
        os.close();
        try {
            reader.readBoolean();
            fail("Expected EOF. Not enough bytes written to stream.");
        } catch (CorruptVomStreamException e) {
            // expected.
        }
    }

    public void testEofBytes() throws IOException {
        writer.writeUint(6);
        os.close();
        try {
            reader.readRawBytes(10);
            fail("Expected EOF. Not enough bytes written to stream.");
        } catch (CorruptVomStreamException e) {
            // expected.
        }
    }

    public void testUint() throws IOException {
        List<Long> tests = new ArrayList<Long>();
        for (int i = 0; i < 64; i++) {
            tests.add((long) (1 << (i - 1)));
            tests.add((long) (0xff << (i - 1)));
            tests.add(-(long) (1 << (i - 1)));
        }
        tests.add((long) -1);
        for (long test : tests) {
            writer.writeUint(test);
            long output = reader.readUint();
            assertEquals(test, output);
        }
    }

    public void testInt() throws IOException {
        List<Long> tests = new ArrayList<Long>();
        for (int i = 0; i < 64; i++) {
            tests.add((long) (1 << (i - 1)));
            tests.add(-(long) (1 << (i)));
            tests.add((long) (0x84 << (i - 1)));
            tests.add(-(long) (0x84 << (i)));
        }
        for (long test : tests) {
            writer.writeInt(test);
            long output = reader.readInt();
            assertEquals(test, output);
        }
    }

    public void testFloat() throws IOException {
        double[] tests = new double[] {
                0.0,
                1.0,
                -1.0,
                0.1,
                -0.1,
                100,
                -100,
                Double.NaN,
                Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY,
                Double.MAX_VALUE,
                Double.MIN_NORMAL,
                Double.MIN_VALUE,
                Float.MAX_VALUE,
                Float.MIN_NORMAL,
                Float.MIN_VALUE,
        };
        for (double test : tests) {
            writer.writeFloat(test);
            double output = reader.readFloat();
            assertEquals(Double.doubleToLongBits(test), Double.doubleToLongBits(output));
        }
    }

    public void testBoolean() throws IOException {
        writer.writeBoolean(true);
        assertEquals(true, reader.readBoolean());

        writer.writeBoolean(false);
        assertEquals(false, reader.readBoolean());

        os.write(7);
        try {
            reader.readBoolean();
            fail("Expected to fail upon receiving invalid boolean value.");
        } catch (CorruptVomStreamException e) {
            // expected
        }
    }

    public void testBytes() throws IOException {
        byte[][] tests = new byte[][] {
                new byte[] {},
                new byte[] {
                    0
                },
                new byte[] {
                        0, 1, 2, 3, 4, 5, 6, 7, 8, 9
                },
                new byte[1000],
        };
        for (byte[] test : tests) {
            writer.writeRawBytes(test);
            assertTrue(Arrays.equals(test, reader.readRawBytes(test.length)));
        }
    }

    public void testString() throws IOException {
        String[] tests = new String[] {
                "",
                "BasicString",
                "Unicode Chars: ซณ🜲🝮ض",
                new String(new char[1000]),
        };
        for (String test : tests) {
            writer.writeString(test);
            assertEquals(test, reader.readString());
        }
    }
}
