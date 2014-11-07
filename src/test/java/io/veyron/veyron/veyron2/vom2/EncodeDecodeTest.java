package io.veyron.veyron.veyron2.vom2;

import junit.framework.TestCase;

import org.apache.commons.math3.complex.Complex;

import io.veyron.veyron.veyron.testing.BufferedPipedInputStream;
import io.veyron.veyron.veyron2.vdl.Kind;
import io.veyron.veyron.veyron2.vdl.VdlStructField;
import io.veyron.veyron.veyron2.vdl.VdlType;
import io.veyron.veyron.veyron2.vdl.Types;
import io.veyron.veyron.veyron2.vom2.Decoder.UnexpectedKindException;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Tests VOM encoding and decoding. This only tests the Java encoding methods
 * and does not compare to known examples of the format.
 */
public class EncodeDecodeTest extends TestCase {
    private Encoder encoder;
    private Decoder decoder;

    @Override
    public void setUp() throws IOException {
        BufferedPipedInputStream is = new BufferedPipedInputStream();
        OutputStream os = is.getOutputStream();
        encoder = new Encoder(os);
        decoder = new Decoder(is);
    }

    public void testBool() throws IOException, CorruptVomStreamException {
        encoder.writeBool(true, Types.BOOL);
        assertTrue(decoder.readBool());
        encoder.writeBool(false, Types.named("BoolName", Types.BOOL));
        assertFalse(decoder.readBool());
        encoder.writeInt32(-1, Types.INT32);
        try {
            decoder.readBool();
        } catch (UnexpectedKindException e) {
            // expected
        }
    }

    public void testString() throws IOException, CorruptVomStreamException {
        encoder.writeString("", Types.STRING);
        assertEquals("", decoder.readString());
        encoder.writeString(null, Types.STRING);
        assertEquals("", decoder.readString());
        encoder.writeString("abcdefgh", Types.STRING);
        assertEquals("abcdefgh", decoder.readString());
        encoder.writeString("named", Types.named("NamedString", Types.STRING));
        assertEquals("named", decoder.readString());
        encoder.writeString("Unicode Chars: ซณ🜲🝮ض", Types.STRING);
        assertEquals("Unicode Chars: ซณ🜲🝮ض", decoder.readString());
        encoder.writeInt32(-1, Types.INT32);
        try {
            decoder.readString();
        } catch (UnexpectedKindException e) {
            // expected
        }
    }

    public void testReadUint() throws IOException, CorruptVomStreamException, ConversionException {
        // Basic same-type writes.
        encoder.writeUint64(10, Types.UINT64);
        assertEquals(10, decoder.readUint64());
        encoder.writeUint32(10, Types.UINT32);
        assertEquals(10, decoder.readUint32());
        encoder.writeUint16((short) 10, Types.UINT16);
        assertEquals(10, decoder.readUint16());

        // Max values:
        encoder.writeUint64(-1L, Types.UINT64);
        assertEquals(-1L, decoder.readUint64());
        encoder.writeUint32(-1, Types.UINT32);
        assertEquals(-1, decoder.readUint32());
        encoder.writeUint16((short) (-1), Types.UINT16);
        assertEquals(-1, decoder.readUint16());

        // Bit size conversion.
        encoder.writeUint64(2, Types.UINT64);
        assertEquals(2, decoder.readUint32());
        encoder.writeUint64(2, Types.UINT64);
        assertEquals(2, decoder.readUint16());
        encoder.writeUint64(Long.MAX_VALUE, Types.UINT64);
        try {
            decoder.readUint32();
            fail("Expected to overflow");
        } catch (ConversionException e) {
            // expected.
        }
        encoder.writeUint32(Integer.MAX_VALUE, Types.UINT32);
        try {
            decoder.readUint16();
            fail("Expected to overflow");
        } catch (ConversionException e) {
            // expected.
        }
        encoder.writeUint16((short) -1, Types.UINT16);
        assertEquals(2 * (long) Short.MAX_VALUE + 1, decoder.readUint64());
        encoder.writeUint32(-1, Types.UINT32);
        assertEquals(2 * (long) Integer.MAX_VALUE + 1, decoder.readUint64());

        // Conversion from int.
        encoder.writeInt64(10, Types.INT64);
        assertEquals(10, decoder.readUint32());
        encoder.writeInt64(-1, Types.INT64);
        try {
            decoder.readUint32();
            fail("Expected conversion exception");
        } catch (ConversionException e) {
            // expected.
        }

        // Conversion from float.
        encoder.writeFloat32(9.0f, Types.FLOAT32);
        assertEquals(9, decoder.readUint16());
        encoder.writeFloat64(10.0, Types.FLOAT64);
        assertEquals(10, decoder.readUint64());
        encoder.writeFloat64(9.5, Types.FLOAT64);
        try {
            decoder.readUint64();
            fail("Expected conversion from decimal float to fail");
        } catch (ConversionException e) {
            // expected.
        }
        encoder.writeFloat64(Double.MAX_VALUE, Types.FLOAT64);
        try {
            decoder.readUint64();
            fail("Expected conversion from large double to fail");
        } catch (ConversionException e) {
            // expected.
        }
        encoder.writeFloat64(Double.MIN_VALUE, Types.FLOAT64);
        try {
            decoder.readUint64();
            fail("Expected conversion from small double to fail");
        } catch (ConversionException e) {
            // expected.
        }

        // Conversion from complex.
        encoder.writeComplex64(new Complex(8, 0), Types.COMPLEX64);
        assertEquals(8, decoder.readUint64());
        encoder.writeComplex128(new Complex(1, 1), Types.COMPLEX128);
        try {
            decoder.readUint32();
            fail("Expected conversion from complex with imaginary part to fail.");
        } catch (ConversionException e) {
            // expected.
        }

        // Named types.
        encoder.writeComplex64(new Complex(99, 0), Types.named("ComplexName", Types.COMPLEX64));
        assertEquals(99, decoder.readUint32());
    }

    public void testReadInt() throws IOException, CorruptVomStreamException, ConversionException {
        // Basic same-type writes.
        encoder.writeInt64(10, Types.INT64);
        assertEquals(10, decoder.readInt64());
        encoder.writeInt32(10, Types.INT32);
        assertEquals(10, decoder.readInt32());
        encoder.writeInt16((short) 10, Types.INT16);
        assertEquals(10, decoder.readInt16());

        // Negative values:
        encoder.writeInt64(-1L, Types.INT64);
        assertEquals(-1L, decoder.readInt64());
        encoder.writeInt32(-1, Types.INT32);
        assertEquals(-1, decoder.readInt32());
        encoder.writeInt16((short) (-1), Types.INT16);
        assertEquals(-1, decoder.readInt16());

        // Bit size conversion.
        encoder.writeInt64(2, Types.INT64);
        assertEquals(2, decoder.readUint32());
        encoder.writeInt64(2, Types.INT64);
        assertEquals(2, decoder.readUint16());
        encoder.writeInt64(Long.MAX_VALUE, Types.INT64);
        try {
            decoder.readInt32();
            fail("Expected to overflow");
        } catch (ConversionException e) {
            // expected.
        }
        encoder.writeInt32(Integer.MAX_VALUE, Types.INT32);
        try {
            decoder.readInt16();
            fail("Expected to overflow");
        } catch (ConversionException e) {
            // expected.
        }

        // Conversion from uint.
        encoder.writeUint64(10, Types.UINT64);
        assertEquals(10, decoder.readInt32());
        encoder.writeUint64(-1, Types.UINT64);
        try {
            decoder.readInt32();
            fail("Expected conversion exception");
        } catch (ConversionException e) {
            // expected.
        }

        // Conversion from float.
        encoder.writeFloat32(9.0f, Types.FLOAT32);
        assertEquals(9, decoder.readInt16());
        encoder.writeFloat64(10.0, Types.FLOAT64);
        assertEquals(10, decoder.readInt64());
        encoder.writeFloat64(9.5, Types.FLOAT64);
        try {
            decoder.readInt64();
            fail("Expected conversion from decimal float to fail");
        } catch (ConversionException e) {
            // expected.
        }
        encoder.writeFloat64(Double.MAX_VALUE, Types.FLOAT64);
        try {
            decoder.readInt64();
            fail("Expected conversion from large double to fail");
        } catch (ConversionException e) {
            // expected.
        }
        encoder.writeFloat64(Double.MIN_VALUE, Types.FLOAT64);
        try {
            decoder.readInt64();
            fail("Expected conversion from small double to fail");
        } catch (ConversionException e) {
            // expected.
        }

        // Conversion from complex.
        encoder.writeComplex64(new Complex(-8, 0), Types.COMPLEX64);
        assertEquals(-8, decoder.readInt64());
        encoder.writeComplex128(new Complex(1, 1), Types.COMPLEX128);
        try {
            decoder.readInt32();
            fail("Expected conversion from complex with imaginary part to fail.");
        } catch (ConversionException e) {
            // expected.
        }

        // Named types.
        encoder.writeComplex64(new Complex(99, 0), Types.named("ComplexName", Types.COMPLEX64));
        assertEquals(99, decoder.readInt32());
    }

    private static void assertFloatEquals(double x, double y) {
        assertEquals(Double.doubleToLongBits(x), Double.doubleToLongBits(y));
    }

    public void testReadFloat() throws IOException, ConversionException {
        // Basic reads.
        encoder.writeFloat32(8.0f, Types.FLOAT32);
        assertFloatEquals(8.0f, decoder.readFloat32());
        encoder.writeFloat64(-10.0, Types.FLOAT64);
        assertFloatEquals(-10.0, decoder.readFloat64());

        // Bit changes.
        encoder.writeFloat32(8.0f, Types.FLOAT32);
        assertFloatEquals(8.0, decoder.readFloat64());
        encoder.writeFloat64(10.0, Types.FLOAT64);
        assertFloatEquals(10.0f, decoder.readFloat32());

        // Extreme values.
        encoder.writeFloat32(Float.MAX_VALUE, Types.FLOAT32);
        encoder.writeFloat32(Float.MIN_VALUE, Types.FLOAT32);
        encoder.writeFloat32(Float.NaN, Types.FLOAT32);
        assertFloatEquals(Float.MAX_VALUE, decoder.readFloat32());
        assertFloatEquals(Float.MIN_VALUE, decoder.readFloat32());
        assertFloatEquals(Float.NaN, decoder.readFloat32());
        encoder.writeFloat64(Double.MAX_VALUE, Types.FLOAT64);
        encoder.writeFloat64(Double.MIN_VALUE, Types.FLOAT64);
        encoder.writeFloat64(Double.NEGATIVE_INFINITY, Types.FLOAT64);
        encoder.writeFloat64(Double.POSITIVE_INFINITY, Types.FLOAT64);
        assertFloatEquals(Double.MAX_VALUE, decoder.readFloat64());
        assertFloatEquals(Double.MIN_VALUE, decoder.readFloat64());
        assertFloatEquals(Double.NEGATIVE_INFINITY, decoder.readFloat64());
        assertFloatEquals(Double.POSITIVE_INFINITY, decoder.readFloat64());

        // Conversions from integers.
        encoder.writeUint32(10, Types.UINT32);
        assertFloatEquals(10.0, decoder.readFloat64());
        encoder.writeInt16((short) -5, Types.INT16);
        assertFloatEquals(-5.0, decoder.readFloat32());
        encoder.writeUint64(-1, Types.UINT64);
        try {
            decoder.readFloat64();
            fail("Conversion should fail. Large uint values out of range of float64.");
        } catch (ConversionException e) {
            // expected.
        }
        encoder.writeUint64(1L << 30, Types.UINT64);
        try {
            decoder.readFloat32();
            fail("Conversion should fail. Large uint values out of range of float32.");
        } catch (ConversionException e) {
            // expected.
        }

        // Conversions from complex.
        encoder.writeComplex64(new Complex(6.0, 0.0), Types.COMPLEX64);
        assertFloatEquals(6.0, decoder.readFloat64());
        encoder.writeComplex128(new Complex(-60.0, 0.0), Types.COMPLEX128);
        assertFloatEquals(-60.0f, decoder.readFloat32());
        encoder.writeComplex64(new Complex(6.0, -1.0), Types.COMPLEX64);
        try {
            decoder.readFloat64();
            fail("Conversion of complex value with imaginary part should fail.");
        } catch (ConversionException e) {
            // expected.
        }

        // Named float.
        encoder.writeFloat64(9.0, Types.named("NameOfFloat", Types.FLOAT64));
        assertFloatEquals(9.0, decoder.readFloat64());
    }

    public void testReadComplex() throws IOException, ConversionException {
        // Basic reads.
        encoder.writeComplex64(new Complex(6.0, -3.0), Types.COMPLEX64);
        assertEquals(new Complex(6.0, -3.0), decoder.readComplex64());
        encoder.writeComplex128(new Complex(6.0, -3.0), Types.COMPLEX128);
        assertEquals(new Complex(6.0, -3.0), decoder.readComplex128());

        // Test changing bit size.
        encoder.writeComplex64(new Complex(6.0, -3.0), Types.COMPLEX64);
        assertEquals(new Complex(6.0, -3.0), decoder.readComplex128());
        encoder.writeComplex128(new Complex(6.0, -3.0), Types.COMPLEX128);
        assertEquals(new Complex(6.0, -3.0), decoder.readComplex64());

        // Test converting from int and float.
        encoder.writeFloat64(-1.0, Types.FLOAT64);
        assertEquals(new Complex(-1.0, 0.0), decoder.readComplex64());
        encoder.writeInt32(-1, Types.INT32);
        assertEquals(new Complex(-1.0, 0.0), decoder.readComplex128());
        encoder.writeUint64(10, Types.UINT64);
        assertEquals(new Complex(10.0, 0.0), decoder.readComplex64());
        encoder.writeUint64(-1, Types.UINT64);
        try {
            decoder.readComplex128();
            fail("Should not be able to convert negative uint to complex values");
        } catch (ConversionException e) {
            // expected.
        }

        // Named complex.
        encoder.writeComplex64(new Complex(4, 0),
                Types.named("Complex", Types.named("ComplexName", Types.COMPLEX64)));
        assertEquals(new Complex(4, 0), decoder.readComplex64());
    }

    public void testReadArray() throws IOException, ConversionException {
        // Empty array.
        encoder.arrayStart(Types.arrayOf(0, Types.COMPLEX64));
        encoder.arrayEnd();
        decoder.arrayStart();
        decoder.arrayEnd();

        // Array with values.
        encoder.arrayStart(Types.arrayOf(5, Types.UINT32));
        for (int i = 0; i < 5; i++) {
            encoder.writeUint32(i, Types.UINT32);
        }
        encoder.arrayEnd();

        decoder.arrayStart();
        for (int i = 0; i < 5; i++) {
            assertEquals(i, decoder.readUint32());
        }
        decoder.arrayEnd();

        // Array in array (with name).
        encoder.arrayStart(Types.arrayOf(2, Types.named("Name", Types.arrayOf(1, Types.STRING))));
        encoder.arrayStart(Types.named("Name", Types.arrayOf(1, Types.STRING)));
        encoder.writeString("A", Types.STRING);
        encoder.arrayEnd();
        encoder.arrayStart(Types.named("Name", Types.arrayOf(1, Types.STRING)));
        encoder.writeString("B", Types.STRING);
        encoder.arrayEnd();
        encoder.arrayEnd();

        decoder.arrayStart();
        decoder.arrayStart();
        assertEquals("A", decoder.readString());
        decoder.arrayEnd();
        decoder.arrayStart();
        assertEquals("B", decoder.readString());
        decoder.arrayEnd();
        decoder.arrayEnd();
    }

    public void testReadList() throws IOException, ConversionException {
        // Empty list.
        encoder.listStart(0, Types.listOf(Types.COMPLEX64));
        encoder.listEnd();
        assertEquals(0, decoder.listStart());
        decoder.listEnd();

        // List with values.
        encoder.listStart(5, Types.listOf(Types.INT32));
        for (int i = 0; i < 5; i++) {
            encoder.writeInt32(i, Types.INT32);
        }
        encoder.listEnd();

        assertEquals(5, decoder.listStart());
        for (int i = 0; i < 5; i++) {
            assertEquals(i, decoder.readUint32());
        }
        decoder.listEnd();

        // List in list (with name).
        encoder.listStart(2, Types.listOf(Types.named("Name", Types.listOf(Types.STRING))));
        encoder.listStart(2, Types.named("Name", Types.listOf(Types.STRING)));
        encoder.writeString("A", Types.STRING);
        encoder.writeString("B", Types.STRING);
        encoder.listEnd();
        encoder.listStart(1, Types.named("Name", Types.listOf(Types.STRING)));
        encoder.writeString("C", Types.STRING);
        encoder.listEnd();
        encoder.listEnd();

        assertEquals(2, decoder.listStart());
        assertEquals(2, decoder.listStart());
        assertEquals("A", decoder.readString());
        assertEquals("B", decoder.readString());
        decoder.listEnd();
        assertEquals(1, decoder.listStart());
        assertEquals("C", decoder.readString());
        decoder.listEnd();
        decoder.listEnd();
    }

    public void testReadSet() throws IOException, ConversionException {
        // Empty set.
        encoder.setStart(0, Types.setOf(Types.FLOAT64));
        encoder.setEnd();
        assertEquals(0, decoder.setStart());
        decoder.setEnd();

        // Set with values.
        encoder.setStart(5, Types.setOf(Types.COMPLEX64));
        for (int i = 0; i < 5; i++) {
            encoder.writeComplex64(new Complex(i), Types.COMPLEX64);
        }
        encoder.setEnd();

        assertEquals(5, decoder.setStart());
        for (int i = 0; i < 5; i++) {
            assertFloatEquals(i, decoder.readFloat64());
        }
        decoder.setEnd();

        // Set in set (with name).
        encoder.setStart(2, Types.setOf(Types.named("Name", Types.setOf(Types.STRING))));
        encoder.setStart(2, Types.named("Name", Types.setOf(Types.STRING)));
        encoder.writeString("A", Types.STRING);
        encoder.writeString("B", Types.STRING);
        encoder.setEnd();
        encoder.setStart(1, Types.named("Name", Types.setOf(Types.STRING)));
        encoder.writeString("C", Types.STRING);
        encoder.setEnd();
        encoder.setEnd();

        assertEquals(2, decoder.setStart());
        assertEquals(2, decoder.setStart());
        assertEquals("A", decoder.readString());
        assertEquals("B", decoder.readString());
        decoder.setEnd();
        assertEquals(1, decoder.setStart());
        assertEquals("C", decoder.readString());
        decoder.setEnd();
        decoder.setEnd();
    }

    public void testReadMap() throws IOException, ConversionException {
        // Empty map.
        encoder.mapStart(0, Types.mapOf(Types.FLOAT64, Types.COMPLEX128));
        encoder.mapEnd();
        assertEquals(0, decoder.mapStart());
        decoder.mapEnd();

        // Map with values.
        encoder.mapStart(5, Types.mapOf(Types.COMPLEX64, Types.STRING));
        for (int i = 0; i < 5; i++) {
            encoder.writeComplex64(new Complex(i), Types.COMPLEX64);
            encoder.writeString("" + (char) (i + 'a'), Types.STRING);
        }
        encoder.mapEnd();

        assertEquals(5, decoder.mapStart());
        for (int i = 0; i < 5; i++) {
            decoder.mapStartKey();
            assertFloatEquals(i, decoder.readFloat64());
            decoder.mapEndKeyStartElem();
            assertEquals("" + (char) (i + 'a'), decoder.readString());
        }
        decoder.mapEnd();

        // Map in map (with name).
        encoder.mapStart(
                1,
                Types.mapOf(Types.named("Name", Types.mapOf(Types.STRING, Types.INT32)),
                        Types.arrayOf(1, Types.STRING)));
        encoder.mapStart(2, Types.named("Name", Types.mapOf(Types.STRING, Types.INT32)));
        encoder.writeString("A", Types.STRING);
        encoder.writeInt32(9, Types.INT32);
        encoder.writeString("B", Types.STRING);
        encoder.writeInt32(3, Types.INT32);
        encoder.mapEnd();
        encoder.arrayStart(Types.arrayOf(1, Types.STRING));
        encoder.writeString("X", Types.STRING);
        encoder.arrayEnd();
        encoder.mapEnd();

        assertEquals(1, decoder.mapStart());
        decoder.mapStartKey();
        assertEquals(2, decoder.mapStart());
        decoder.mapStartKey();
        assertEquals("A", decoder.readString());
        decoder.mapEndKeyStartElem();
        assertEquals(9, decoder.readInt32());
        decoder.mapStartKey();
        assertEquals("B", decoder.readString());
        decoder.mapEndKeyStartElem();
        assertEquals(3, decoder.readInt32());
        decoder.mapEnd();
        decoder.mapEndKeyStartElem();
        decoder.arrayStart();
        assertEquals("X", decoder.readString());
        decoder.arrayEnd();
        decoder.mapEnd();
    }

    public void testReadStruct() throws IOException, ConversionException {
        // Empty struct
        encoder.structStart(Types.structOf());
        encoder.structEnd();
        decoder.structStart();
        decoder.structEnd();

        // Simple struct.
        encoder.structStart(Types.structOf(new VdlStructField("A", Types.BOOL),
                new VdlStructField("B", Types.STRING)));
        encoder.structNextField("A");
        encoder.writeBool(true, Types.BOOL);
        encoder.structNextField("B");
        encoder.writeString("X", Types.STRING);
        encoder.structEnd();

        decoder.structStart();
        assertEquals("A", decoder.structNextField());
        assertEquals(true, decoder.readBool());
        assertEquals("B", decoder.structNextField());
        assertEquals("X", decoder.readString());
        decoder.structEnd();

        // Fields out of order.
        encoder.structStart(Types.structOf(new VdlStructField("A", Types.BOOL),
                new VdlStructField("B", Types.STRING)));
        encoder.structNextField("B");
        encoder.writeString("X", Types.STRING);
        encoder.structNextField("A");
        encoder.writeBool(true, Types.BOOL);
        encoder.structEnd();

        decoder.structStart();
        assertEquals("B", decoder.structNextField());
        assertEquals("X", decoder.readString());
        assertEquals("A", decoder.structNextField());
        assertEquals(true, decoder.readBool());
        decoder.structEnd();

        // Struct in struct.
        encoder.structStart(Types.structOf(new VdlStructField("S", Types.named("Name",
                Types.structOf(new VdlStructField("W", Types.UINT32))))));
        encoder.structNextField("S");
        encoder.structStart(Types.named("Name",
                Types.structOf(new VdlStructField("W", Types.UINT32))));
        encoder.structNextField("W");
        encoder.writeUint32(9, Types.UINT32);
        encoder.structEnd();
        encoder.structEnd();

        decoder.structStart();
        assertEquals("S", decoder.structNextField());
        decoder.structStart();
        assertEquals("W", decoder.structNextField());
        assertEquals(9, decoder.readUint32());
        decoder.structEnd();
        decoder.structEnd();
    }

    public void testRecursiveTypes() throws IOException {
        VdlType selfReferencingArray = new VdlType(Kind.ARRAY);
        selfReferencingArray.setLength(0);
        selfReferencingArray.setElem(selfReferencingArray);
        encoder.arrayStart(selfReferencingArray);
        encoder.arrayEnd();
        decoder.arrayStart();
        assertEquals(selfReferencingArray, decoder.currentType());
        decoder.arrayEnd();

        VdlType level2RecStruct = new VdlType(Kind.STRUCT);
        VdlType level2RecList = new VdlType(Kind.LIST);
        level2RecList.setElem(level2RecStruct);
        level2RecStruct.setFields(new VdlStructField("list", level2RecList));
        encoder.listStart(1, level2RecList);
        encoder.structStart(level2RecStruct);
        encoder.structNextField("list");
        encoder.listStart(0, level2RecList);
        encoder.listEnd();
        encoder.structEnd();
        encoder.listEnd();

        assertEquals(1, decoder.listStart());
        decoder.structStart();
        assertEquals("list", decoder.structNextField());
        assertEquals(0, decoder.listStart());
        decoder.listEnd();
        decoder.structEnd();
        decoder.listEnd();
    }
}
