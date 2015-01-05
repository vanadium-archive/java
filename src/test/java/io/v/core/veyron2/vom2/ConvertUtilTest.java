package io.v.core.veyron2.vom2;

import com.google.common.reflect.TypeToken;

import junit.framework.TestCase;

import io.v.core.veyron2.vdl.Types;
import io.v.core.veyron2.vdl.VdlByte;
import io.v.core.veyron2.vdl.VdlComplex128;
import io.v.core.veyron2.vdl.VdlComplex64;
import io.v.core.veyron2.vdl.VdlFloat32;
import io.v.core.veyron2.vdl.VdlFloat64;
import io.v.core.veyron2.vdl.VdlInt16;
import io.v.core.veyron2.vdl.VdlInt32;
import io.v.core.veyron2.vdl.VdlInt64;
import io.v.core.veyron2.vdl.VdlString;
import io.v.core.veyron2.vdl.VdlUint16;
import io.v.core.veyron2.vdl.VdlUint32;
import io.v.core.veyron2.vdl.VdlUint64;
import io.v.core.veyron2.vom2.BinaryDecoder.DecodingMode;
import io.v.core.veyron2.vom2.testdata.NArray2Uint64;
import io.v.core.veyron2.vom2.testdata.NByte;
import io.v.core.veyron2.vom2.testdata.NComplex128;
import io.v.core.veyron2.vom2.testdata.NComplex64;
import io.v.core.veyron2.vom2.testdata.NEnum;
import io.v.core.veyron2.vom2.testdata.NFloat32;
import io.v.core.veyron2.vom2.testdata.NFloat64;
import io.v.core.veyron2.vom2.testdata.NInt16;
import io.v.core.veyron2.vom2.testdata.NInt32;
import io.v.core.veyron2.vom2.testdata.NInt64;
import io.v.core.veyron2.vom2.testdata.NString;
import io.v.core.veyron2.vom2.testdata.NUint16;
import io.v.core.veyron2.vom2.testdata.NUint32;
import io.v.core.veyron2.vom2.testdata.NUint64;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.List;

/**
 * Test cases for conversion rules.
 */
public class ConvertUtilTest extends TestCase {
    private static class NumericConversionTestCase {
        public final Object value;
        public final String typeMask;

        public NumericConversionTestCase(Object value, String typeMask) {
            assert(typeMask.length() == numericTypes.length);
            this.value = value;
            this.typeMask = typeMask;
        }
    }

    private static class BytesConversionTestCase {
        public final String value;
        public final String typeMask;

        public BytesConversionTestCase(String value, String typeMask) {
            assert(typeMask.length() == bytesTypes.length);
            this.value = value;
            this.typeMask = typeMask;
        }
    }

    private static final Class<?>[][] numericTypes = {
        {Byte.TYPE, Byte.class, VdlByte.class, NByte.class},
        {VdlUint16.class, NUint16.class},
        {VdlUint32.class, NUint32.class},
        {VdlUint64.class, NUint64.class},
        {Short.TYPE, Short.class, VdlInt16.class, NInt16.class},
        {Integer.TYPE, Integer.class, VdlInt32.class, NInt32.class},
        {Long.TYPE, Long.class, VdlInt64.class, NInt64.class},
        {Float.TYPE, Float.class, VdlFloat32.class, NFloat32.class},
        {Double.TYPE, Double.class, VdlFloat64.class, NFloat64.class},
        {NComplex64.class, VdlComplex64.class},
        {NComplex128.class, VdlComplex128.class}
    };

    private static final NumericConversionTestCase[] numericTests = {
        // bytes
        new NumericConversionTestCase((byte) 0,                                 "11111111111"),
        new NumericConversionTestCase((byte) 0xf,                               "11111111111"),
        new NumericConversionTestCase((byte) 0xff,                              "11111111111"),
        // uints
        new NumericConversionTestCase(new VdlUint64(0L),                        "11111111111"),
        new NumericConversionTestCase(new VdlUint64(0xfL),                      "11111111111"),
        new NumericConversionTestCase(new VdlUint64(0xffL),                     "11111111111"),
        new NumericConversionTestCase(new VdlUint64(0xfffL),                    "01111111111"),
        new NumericConversionTestCase(new VdlUint64(0x7ffL),                    "01111111111"),
        new NumericConversionTestCase(new VdlUint64(0xffffL),                   "01110111111"),
        new NumericConversionTestCase(new VdlUint64(0xffffffL),                 "00110111111"),
        new NumericConversionTestCase(new VdlUint64(0x1000000L),                "00110111111"),
        new NumericConversionTestCase(new VdlUint64(0x1000001L),                "00110110101"),
        new NumericConversionTestCase(new VdlUint64(0x7fffffffL),               "00110110101"),
        new NumericConversionTestCase(new VdlUint64(0xffffffffL),               "00110010101"),
        new NumericConversionTestCase(new VdlUint64(0xffffffffffffL),           "00010010101"),
        new NumericConversionTestCase(new VdlUint64(0x20000000000000L),         "00010010101"),
        new NumericConversionTestCase(new VdlUint64(0x20000000000001L),         "00010010000"),
        new NumericConversionTestCase(new VdlUint64(0x7fffffffffffffffL),       "00010010000"),
        new NumericConversionTestCase(new VdlUint64(0xffffffffffffffffL),       "00010000000"),
        // positive ints
        new NumericConversionTestCase(0L,                                       "11111111111"),
        new NumericConversionTestCase(0xfL,                                     "11111111111"),
        new NumericConversionTestCase(0xffL,                                    "11111111111"),
        new NumericConversionTestCase(0xfffL,                                   "01111111111"),
        new NumericConversionTestCase(0x7ffL,                                   "01111111111"),
        new NumericConversionTestCase(0xffffL,                                  "01110111111"),
        new NumericConversionTestCase(0xffffffL,                                "00110111111"),
        new NumericConversionTestCase(0x1000000L,                               "00110111111"),
        new NumericConversionTestCase(0x1000001L,                               "00110110101"),
        new NumericConversionTestCase(0x7fffffffL,                              "00110110101"),
        new NumericConversionTestCase(0xffffffffL,                              "00110010101"),
        new NumericConversionTestCase(0xffffffffffffL,                          "00010010101"),
        new NumericConversionTestCase(0x20000000000000L,                        "00010010101"),
        new NumericConversionTestCase(0x7fffffffffffffffL,                      "00010010000"),
        // negative ints
        new NumericConversionTestCase(-1L,                                      "00001111111"),
        new NumericConversionTestCase(-0x10L,                                   "00001111111"),
        new NumericConversionTestCase(-0x80L,                                   "00001111111"),
        new NumericConversionTestCase(-0x800L,                                  "00001111111"),
        new NumericConversionTestCase(-0x8000L,                                 "00001111111"),
        new NumericConversionTestCase(-0x800000L,                               "00000111111"),
        new NumericConversionTestCase(-0x80000000L,                             "00000110101"),
        new NumericConversionTestCase(-0x800000000000L,                         "00000010101"),
        new NumericConversionTestCase(-0x20000000000000L,                       "00000010101"),
        new NumericConversionTestCase(-0x8000000000000000L,                     "00000010000"),
        // positive floats
        new NumericConversionTestCase(1e0f,                                     "11111111111"),
        new NumericConversionTestCase(1e2f,                                     "11111111111"),
        new NumericConversionTestCase(1e4f,                                     "01111111111"),
        new NumericConversionTestCase(1e8f,                                     "00110111111"),
        new NumericConversionTestCase(1e16f,                                    "00010011111"),
        new NumericConversionTestCase(1e19f,                                    "00010001111"),
        new NumericConversionTestCase(1e20f,                                    "00000001111"),
        new NumericConversionTestCase(1.1e0f,                                   "00000001111"),
        // negative doubles
        new NumericConversionTestCase(-1e0f,                                    "00001111111"),
        new NumericConversionTestCase(-1e2f,                                    "00001111111"),
        new NumericConversionTestCase(-1e4f,                                    "00001111111"),
        new NumericConversionTestCase(-1e8f,                                    "00000111111"),
        new NumericConversionTestCase(-1e16f,                                   "00000011111"),
        new NumericConversionTestCase(-1e20f,                                   "00000001111"),
        new NumericConversionTestCase(-1.1e0f,                                  "00000001111"),
        // complex values
        new NumericConversionTestCase(new VdlComplex128(0f),                    "11111111111"),
        new NumericConversionTestCase(new VdlComplex128(1e8f),                  "00110111111"),
        new NumericConversionTestCase(new VdlComplex128(1e20f),                 "00000001111"),
        new NumericConversionTestCase(new VdlComplex128(1.1e0f),                "00000001111"),
        new NumericConversionTestCase(new VdlComplex128(-1f),                   "00001111111"),
        new NumericConversionTestCase(new VdlComplex128(-1e8f),                 "00000111111"),
        new NumericConversionTestCase(new VdlComplex128(-1e20f),                "00000001111"),
        new NumericConversionTestCase(new VdlComplex128(-1.1e0f),               "00000001111"),
        new NumericConversionTestCase(new VdlComplex128(0f, 1f),                "00000000011"),
    };

    private static final Type[][] bytesTypes = {
        {byte[].class, int[].class, Byte[].class, Integer[].class, VdlUint16[].class, String.class,
                VdlString.class, NString.class,
                new TypeToken<List<Byte>>(){}.getType(), new TypeToken<List<Integer>>(){}.getType(),
                new TypeToken<List<VdlUint16>>(){}.getType()},
        {NArray2Uint64.class},
        {NEnum.class},
    };

    private static final BytesConversionTestCase[] bytesTests = {
        new BytesConversionTestCase("A",         "111"),
        new BytesConversionTestCase("B",         "111"),
        new BytesConversionTestCase("C",         "111"),
        new BytesConversionTestCase("D",         "110"),
        new BytesConversionTestCase("AA",        "110"),
        new BytesConversionTestCase("BB",        "110"),
        new BytesConversionTestCase("CC",        "110"),
        new BytesConversionTestCase("DD",        "110"),
        new BytesConversionTestCase("AAA",       "100"),
        new BytesConversionTestCase("BBB",       "100"),
        new BytesConversionTestCase("CCC",       "100"),
        new BytesConversionTestCase("DDD",       "100")
    };

    private Object convertValue(Object value, ConversionTarget target) {
        try {
            if (value instanceof Byte) {
                return ConvertUtil.convertFromByte((Byte) value, target);
            } else if (value instanceof VdlUint64) {
                return ConvertUtil.convertFromUint(((VdlUint64) value).getValue(), target);
            } else if (value instanceof Long) {
                return ConvertUtil.convertFromInt((Long) value, target);
            } else if (value instanceof Float) {
                return ConvertUtil.convertFromDouble((Float) value, target);
            } else if (value instanceof VdlComplex128) {
                double real = ((VdlComplex128) value).getReal();
                double imag = ((VdlComplex128) value).getImag();
                return ConvertUtil.convertFromComplex(real, imag, target);
            } else {
                throw new IllegalArgumentException("Unexpected value " + value);
            }
        } catch (ConversionException e) {
            return null;
        }
    }

    private BigDecimal normalizeInt(long value) {
        return BigDecimal.valueOf(value);
    }

    private BigDecimal normalizeUint(long value) {
        BigDecimal result = normalizeInt((value << 1) >>> 1);
        if (value < 0) {
            result = result.add(BigDecimal.valueOf(2).pow(63));
        }
        return result;
    }

    private BigDecimal normalizeDouble(double value) {
        // this is to make sure that values like 1e19f and (long) 1e19f have the same representation
        int power = 0;
        while (value > 1e15) {
            value /= 2;
            power++;
        }
        return BigDecimal.valueOf(value).multiply(BigDecimal.valueOf(2).pow(power));
    }

    private BigDecimal normalizeComplex(VdlComplex128 value) {
        BigDecimal result = normalizeDouble(value.getReal());
        if (value.getImag() !=0 ) {
            result = result.add(normalizeDouble(value.getImag()).multiply(BigDecimal.TEN.pow(100)));
        }
        return result;
    }

    private BigDecimal normalize(Object value) {
        if (value instanceof Byte) {
            return normalizeUint(((Byte) value) & 0xffL);
        } else if (value instanceof VdlByte) {
            return normalizeUint(((VdlByte) value).getValue() & 0xffL);
        } else if (value instanceof VdlUint16) {
            return normalizeUint(((VdlUint16) value).getValue() & 0xffffL);
        } else if (value instanceof VdlUint32) {
            return normalizeUint(((VdlUint32) value).getValue() & 0xffffffffL);
        } else if (value instanceof VdlUint64) {
            return normalizeUint(((VdlUint64) value).getValue());
        } else if (value instanceof Short) {
            return normalizeInt((Short) value);
        } else if (value instanceof Integer) {
            return normalizeInt((Integer) value);
        } else if (value instanceof Long) {
            return normalizeInt((Long) value);
        } else if (value instanceof VdlInt16) {
            return normalizeInt(((VdlInt16) value).getValue());
        } else if (value instanceof VdlInt32) {
            return normalizeInt(((VdlInt32) value).getValue());
        } else if (value instanceof VdlInt64) {
            return normalizeInt(((VdlInt64) value).getValue());
        } else if (value instanceof Float) {
            return normalizeDouble((Float) value);
        } else if (value instanceof Double) {
            return normalizeDouble((Double) value);
        } else if (value instanceof VdlFloat32) {
            return normalizeComplex(new VdlComplex128(((VdlFloat32) value).getValue()));
        } else if (value instanceof VdlFloat64) {
            return normalizeComplex(new VdlComplex128(((VdlFloat64) value).getValue()));
        } else if (value instanceof VdlComplex64) {
            return normalizeComplex(new VdlComplex128(((VdlComplex64) value).getReal(),
                    ((VdlComplex64) value).getImag()));
        } else if (value instanceof VdlComplex128) {
            return normalizeComplex((VdlComplex128) value);
        } else {
            throw new IllegalArgumentException("Unexpected value " + value);
        }
    }


    private void assertNumbersEqual(Object expected, Object actual) {
        assertTrue("Expected " + normalize(expected) + ", was " + normalize(actual),
                normalize(expected).compareTo(normalize(actual)) == 0);
    }

    public void testNumericConversion() {
        for (NumericConversionTestCase test : numericTests) {
            for (int i = 0; i < numericTypes.length; i++) {
                for (Class<?> numericType : numericTypes[i]) {
                    Object result = convertValue(test.value, new ConversionTarget(numericType));
                    Object vdlTypeResult = convertValue(test.value, new ConversionTarget(
                            Types.getVdlTypeFromReflect(numericType), DecodingMode.VDL_VALUE));
                    if (test.typeMask.charAt(i) == '0') {
                        assertNull(result);
                        assertNull(vdlTypeResult);
                    } else {
                        assertNumbersEqual(test.value, result);
                        assertNumbersEqual(test.value, vdlTypeResult);
                    }
                }
            }
        }
    }

    private Object convertString(String value, ConversionTarget target) {
        try {
            return ConvertUtil.convertFromBytes(value.getBytes(), target);
        } catch (ConversionException e) {
            return null;
        }
    }

    public void testBytesConversion() {
        for (BytesConversionTestCase test : bytesTests) {
            for (int i = 0; i < bytesTypes.length; i++) {
                for (Type bytesType : bytesTypes[i]) {
                    Object result = convertString(test.value, new ConversionTarget(bytesType));
                    if (test.typeMask.charAt(i) == '0') {
                        assertNull(result);
                    } else {
                        assertNotNull(result);
                    }
                }
            }
        }
    }
}
