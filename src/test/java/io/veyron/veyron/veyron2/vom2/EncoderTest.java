package io.veyron.veyron.veyron2.vom2;

import com.google.common.collect.ImmutableMap;

import junit.framework.TestCase;

import org.apache.commons.math3.complex.Complex;

import io.veyron.veyron.veyron2.vdl.Kind;
import io.veyron.veyron.veyron2.vdl.VdlType;
import io.veyron.veyron.veyron2.vdl.Types;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * EncoderTest tests the VOM encoder against known expected outputs.
 */
public class EncoderTest extends TestCase {
    private static class VomTestValue {
        public Object val;
        public VdlType type;

        public VomTestValue(Object val, VdlType type) {
            this.val = val;
            this.type = type;
        }
    }

    private static class EncodeTestCase {
        public String name;
        public List<VomTestValue> values;
        public String expectedHex;

        public EncodeTestCase(String name, List<VomTestValue> values,
                String expectedHex) {
            this.name = name;
            this.values = values;
            this.expectedHex = expectedHex;
        }
    }

    private static List<EncodeTestCase> testCases = Arrays
            .asList(new EncodeTestCase("Basic", Arrays
                    .asList(new VomTestValue(true, Types.BOOL),
                            new VomTestValue("abc", Types.STRING),
                            new VomTestValue("def".getBytes(), Types
                                    .listOf(Types.BYTE))),
                    "80060108036162633403646566"),
                    new EncodeTestCase("Uint", Arrays.asList(new VomTestValue(
                            1, Types.BYTE), new VomTestValue(2, Types.UINT16),
                            new VomTestValue(3, Types.UINT32),
                            new VomTestValue(4, Types.UINT64)),
                            "800a010c020e031004"),
                    new EncodeTestCase("Int", Arrays.asList(new VomTestValue(
                            -1, Types.INT16),
                            new VomTestValue(-2, Types.INT32),
                            new VomTestValue(-3, Types.INT64)),
                            "80120114031605"),
                    new EncodeTestCase("Float", Arrays.asList(new VomTestValue(
                            3.0, Types.FLOAT32), new VomTestValue(4.0,
                            Types.FLOAT64)), "8018fe08401afe1040"),
                    new EncodeTestCase("Complex",
                            Arrays.asList(
                                    // Is the testdata_test version what we
                                    // want?
                                    new VomTestValue(new Complex(3, 4),
                                            Types.COMPLEX64),
                                    new VomTestValue(new Complex(5, 6),
                                            Types.COMPLEX128)),
                            "801c06fe0840fe10401e06fe1440fe1840"),
                    new EncodeTestCase(
                            "NamedBool",
                            Arrays.asList(
                                    new VomTestValue(true, Types.named(
                                            "veyron2/vom2.Bool", Types.BOOL)),
                                    new VomTestValue(true, Types.named(
                                            "veyron2/vom2.Bool", Types.BOOL))),
                            "80"
                                    + "ff8117100111766579726f6e322f766f6d322e426f6f6c020300"
                                    + "ff8201ff8201"),
                    new EncodeTestCase(
                            "NamedString",
                            Arrays.asList(
                                    new VomTestValue("abc", Types
                                            .named("veyron2/vom2.String",
                                                    Types.STRING)),
                                    new VomTestValue("abc", Types
                                            .named("veyron2/vom2.String",
                                                    Types.STRING))),
                            "80"
                                    + "ff8119100113766579726f6e322f766f6d322e537472696e67020400"
                                    + "ff8203616263ff8203616263"),
                    new EncodeTestCase("UnnamedArray", Arrays.asList(
                            new VomTestValue(new short[] {
                                    1, 2
                            }, Types
                                    .arrayOf(2, Types.UINT16)),
                            new VomTestValue(new short[] {
                                    1, 2
                            }, Types
                                    .arrayOf(2, Types.UINT16))), "80"
                            + "ff8106120206030200" + "ff82020102ff82020102"),
                    new EncodeTestCase(
                            "Array2Uint16",
                            Arrays.asList(
                                    new VomTestValue(
                                            new short[] {
                                                    1, 2
                                            },
                                            Types.named(
                                                    "veyron2/vom2.Array2Uint16",
                                                    Types.arrayOf(2,
                                                            Types.UINT16))),
                                    new VomTestValue(
                                            new short[] {
                                                    1, 2
                                            },
                                            Types.named(
                                                    "veyron2/vom2.Array2Uint16",
                                                    Types.arrayOf(2,
                                                            Types.UINT16)))),
                            "80"
                                    + "ff8121120119766579726f6e322f766f6d322e41727261793255696e7431360206030200"
                                    + "ff82020102ff82020102"),
                    new EncodeTestCase("UnnamedList", Arrays.asList(
                            new VomTestValue(new short[] {
                                    1, 2
                            }, Types
                                    .listOf(Types.UINT16)),
                            new VomTestValue(new short[] {
                                    1, 2
                            }, Types
                                    .listOf(Types.UINT16))), "80"
                            + "ff810413020600" + "ff8203020102ff8203020102"),
                    new EncodeTestCase(
                            "ListUint16",
                            Arrays.asList(
                                    new VomTestValue(new short[] {
                                            1, 2
                                    },
                                            Types.named(
                                                    "veyron2/vom2.ListUint16",
                                                    Types.listOf(Types.UINT16))),
                                    new VomTestValue(new short[] {
                                            1, 2
                                    },
                                            Types.named(
                                                    "veyron2/vom2.ListUint16",
                                                    Types.listOf(Types.UINT16)))),
                            "80"
                                    + "ff811d130117766579726f6e322f766f6d322e4c69737455696e743136020600"
                                    + "ff8203020102ff8203020102"),
                    new EncodeTestCase("UnnamedSet", Arrays.asList(
                            new VomTestValue(new short[] {
                                    1, 2
                            }, Types
                                    .setOf(Types.UINT16)),
                            new VomTestValue(new short[] {
                                    1, 2
                            }, Types
                                    .setOf(Types.UINT16))), "80"
                            + "ff810414020600"
                            + "ff820302[01,02]ff820302[01,02]"),
                    new EncodeTestCase(
                            "SetUint16",
                            Arrays.asList(
                                    new VomTestValue(new short[] {
                                            1, 2
                                    },
                                            Types.named(
                                                    "veyron2/vom2.SetUint16",
                                                    Types.setOf(Types.UINT16))),
                                    new VomTestValue(new short[] {
                                            1, 2
                                    },
                                            Types.named(
                                                    "veyron2/vom2.SetUint16",
                                                    Types.setOf(Types.UINT16)))),
                            "80"
                                    + "ff811c140116766579726f6e322f766f6d322e53657455696e743136020600"
                                    + "ff820302[01,02]ff820302[01,02]"),
                    new EncodeTestCase("UnnamedMap", Arrays.asList(
                            new VomTestValue(ImmutableMap.of((short) 1, "abc",
                                    (short) 2, "def"), Types.mapOf(
                                    Types.UINT16, Types.STRING)),
                            new VomTestValue(ImmutableMap.of((short) 1, "abc",
                                    (short) 2, "def"), Types.mapOf(
                                    Types.UINT16, Types.STRING))), "80"
                            + "ff8106150206030400"
                            + "ff820b02[0103616263,0203646566]"
                            + "ff820b02[0103616263,0203646566]"),
                    new EncodeTestCase(
                            "MapUint16String",
                            Arrays.asList(
                                    new VomTestValue(
                                            ImmutableMap.of((short) 1, "abc",
                                                    (short) 2, "def"),
                                            Types.named(
                                                    "veyron2/vom2.MapUint16String",
                                                    Types.mapOf(Types.UINT16,
                                                            Types.STRING))),
                                    new VomTestValue(
                                            ImmutableMap.of((short) 1, "abc",
                                                    (short) 2, "def"),
                                            Types.named(
                                                    "veyron2/vom2.MapUint16String",
                                                    Types.mapOf(Types.UINT16,
                                                            Types.STRING)))),
                            "80"
                                    + "ff812415011c766579726f6e322f766f6d322e4d617055696e743136537472696e670206030400"
                                    + "ff820b02[0103616263,0203646566]"
                                    + "ff820b02[0103616263,0203646566]"),
                    new EncodeTestCase("EmptyStruct", Arrays
                            .asList(new VomTestValue(null, Types.structOf())),
                            "80ff810416020000ff8200")

            );

    public void testBasicEncode() throws IOException {
        for (EncodeTestCase testCase : testCases) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            Encoder enc = new Encoder(bos);
            for (VomTestValue testVal : testCase.values) {
                Object val = testVal.val;
                switch (testVal.type.getKind()) {
                    case BOOL:
                        enc.writeBool((Boolean) val, testVal.type);
                        break;
                    case BYTE:
                        enc.writeByte((byte) objectToLong(val), testVal.type);
                        break;
                    case UINT16:
                        enc.writeUint16((short) objectToLong(val), testVal.type);
                        break;
                    case UINT32:
                        enc.writeUint32((int) objectToLong(val), testVal.type);
                        break;
                    case UINT64:
                        enc.writeUint64(objectToLong(val), testVal.type);
                        break;
                    case INT16:
                        enc.writeInt16((short) objectToLong(val), testVal.type);
                        break;
                    case INT32:
                        enc.writeInt32((int) objectToLong(val), testVal.type);
                        break;
                    case INT64:
                        enc.writeInt64(objectToLong(val), testVal.type);
                        break;
                    case FLOAT32:
                        enc.writeFloat32((float) objectToFloat(val), testVal.type);
                        break;
                    case FLOAT64:
                        enc.writeFloat64(objectToFloat(val), testVal.type);
                        break;
                    case COMPLEX64:
                        enc.writeComplex64((Complex) val, testVal.type);
                        break;
                    case COMPLEX128:
                        enc.writeComplex128((Complex) val, testVal.type);
                        break;
                    case STRING:
                        enc.writeString((String) val, testVal.type);
                        break;
                    case ARRAY:
                        if (testVal.type.getElem().getKind() == Kind.BYTE) {
                            enc.writeBytes((byte[]) val, testVal.type);
                            break;
                        } else if (testVal.type.getElem().getKind() == Kind.UINT16) {
                            short[] nums = (short[]) testVal.val;
                            enc.arrayStart(testVal.type);
                            for (short num : nums) {
                                enc.writeUint16(num, testVal.type.getElem());
                            }
                            enc.arrayEnd();
                        } else {
                            throw new RuntimeException("Unexpected kind");
                        }
                        break;
                    case LIST:
                        if (testVal.type.getElem().getKind() == Kind.BYTE) {
                            enc.writeBytes((byte[]) val, testVal.type);
                            break;
                        } else if (testVal.type.getElem().getKind() == Kind.UINT16) {
                            short[] nums = (short[]) testVal.val;
                            enc.listStart(nums.length, testVal.type);
                            for (short num : nums) {
                                enc.writeUint16(num, testVal.type.getElem());
                            }
                            enc.listEnd();
                        } else {
                            throw new RuntimeException("Unexpected kind");
                        }
                        break;
                    case SET:
                        if (testVal.type.getKey().getKind() == Kind.UINT16) {
                            short[] nums = (short[]) testVal.val;
                            enc.setStart(nums.length, testVal.type);
                            for (short num : nums) {
                                enc.writeUint16(num, testVal.type.getKey());
                            }
                            enc.setEnd();
                        } else {
                            throw new RuntimeException("Unexpected kind");
                        }
                        break;
                    case MAP:
                        if (testVal.type.getKey().getKind() == Kind.UINT16
                                && testVal.type.getElem().getKind() == Kind.STRING) {
                            @SuppressWarnings("unchecked")
                            Map<Short, String> m = (Map<Short, String>) testVal.val;
                            enc.setStart(m.size(), testVal.type);
                            for (Map.Entry<Short, String> e : m.entrySet()) {
                                enc.writeUint16(e.getKey(), testVal.type.getKey());
                                enc.writeString(e.getValue(),
                                        testVal.type.getElem());
                            }
                            enc.setEnd();
                        } else {
                            throw new RuntimeException("Unexpected kind");
                        }
                        break;
                    case STRUCT:
                        if (testCase.name.equals("EmptyStruct")) {
                            enc.structStart(testVal.type);
                            enc.structEnd();
                        } else if (testCase.name.equals("Struct")) {
                            enc.structStart(testVal.type);
                            enc.structNextField("A");
                            enc.writeUint64((Integer) testVal.val,
                                    testVal.type.getFields().get(0).getType());
                            enc.structEnd();
                        } else {
                            throw new RuntimeException("Unsupported struct "
                                    + testVal.type.getName());
                        }
                        break;
                    default:
                        throw new RuntimeException("Kind " + testVal.type.getKind()
                                + " not yet implemented");
                }
            }

            byte[] result = bos.toByteArray();
            String hexResult = TestUtil.bytesToHexString(result);
            TestUtil.matchHexString(testCase.name, testCase.expectedHex,
                    hexResult);
        }
    }

    private static long objectToLong(Object obj) {
        if (obj instanceof Byte) {
            return (Byte) obj;
        } else if (obj instanceof Short) {
            return (Short) obj;
        } else if (obj instanceof Integer) {
            return (Integer) obj;
        } else if (obj instanceof Long) {
            return (Long) obj;
        }
        throw new RuntimeException("Invalid integer type");
    }

    private static double objectToFloat(Object obj) {
        if (obj instanceof Float) {
            return (Float) obj;
        } else if (obj instanceof Double) {
            return (Double) obj;
        }
        throw new RuntimeException("Invalid float type");
    }
}
