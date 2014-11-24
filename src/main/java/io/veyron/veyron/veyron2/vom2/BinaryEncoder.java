package io.veyron.veyron.veyron2.vom2;

import io.veyron.veyron.veyron2.vdl.Kind;
import io.veyron.veyron.veyron2.vdl.AbstractVdlStruct;
import io.veyron.veyron.veyron2.vdl.Types;
import io.veyron.veyron.veyron2.vdl.VdlAny;
import io.veyron.veyron.veyron2.vdl.VdlArray;
import io.veyron.veyron.veyron2.vdl.VdlBool;
import io.veyron.veyron.veyron2.vdl.VdlByte;
import io.veyron.veyron.veyron2.vdl.VdlComplex128;
import io.veyron.veyron.veyron2.vdl.VdlComplex64;
import io.veyron.veyron.veyron2.vdl.VdlEnum;
import io.veyron.veyron.veyron2.vdl.VdlFloat32;
import io.veyron.veyron.veyron2.vdl.VdlFloat64;
import io.veyron.veyron.veyron2.vdl.VdlInt16;
import io.veyron.veyron.veyron2.vdl.VdlInt32;
import io.veyron.veyron.veyron2.vdl.VdlInt64;
import io.veyron.veyron.veyron2.vdl.VdlOneOf;
import io.veyron.veyron.veyron2.vdl.VdlString;
import io.veyron.veyron.veyron2.vdl.VdlStruct;
import io.veyron.veyron.veyron2.vdl.VdlStructField;
import io.veyron.veyron.veyron2.vdl.VdlType;
import io.veyron.veyron.veyron2.vdl.VdlTypeObject;
import io.veyron.veyron.veyron2.vdl.VdlUint16;
import io.veyron.veyron.veyron2.vdl.VdlUint32;
import io.veyron.veyron.veyron2.vdl.VdlUint64;
import io.veyron.veyron.veyron2.vdl.VdlValue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * BinaryEncoder writes VDL values to {@code OutputStream} in binary VOM format.
 */
public class BinaryEncoder {
    private final ByteArrayOutputStream valueBuffer;
    private final ByteArrayOutputStream typeBuffer;
    private final OutputStream out;
    private final Map<VdlType, TypeID> visitedTypes;
    private TypeID nextTypeId;
    private boolean binaryMagicByteWritten;

    public BinaryEncoder(OutputStream out) {
        this.valueBuffer = new ByteArrayOutputStream();
        this.typeBuffer = new ByteArrayOutputStream();
        this.out = out;
        this.visitedTypes = new HashMap<VdlType, TypeID>();
        this.nextTypeId = Vom2Constants.WIRE_TYPE_FIRST_USER_ID;
        this.binaryMagicByteWritten = false;
    }

    /**
     * Encodes a value into binary VOM format.
     *
     * @param type runtime VDL type of the value
     * @param value the value to encode
     * @throws IOException
     */
    public void encodeValue(VdlType type, Object value) throws IOException {
        if (!binaryMagicByteWritten) {
            binaryMagicByteWritten = true;
            out.write(BinaryUtil.BINARY_MAGIC_BYTE);
        }
        valueBuffer.reset();
        TypeID typeId = getType(type);
        writeValue(valueBuffer, value, type);
        writeMessage(valueBuffer, typeId.getValue(), BinaryUtil.hasBinaryMsgLen(type));
    }

    /**
     * Encodes a value into binary VOM format.
     *
     * @param type runtime {@code  java.lang.reflectType} of the value
     * @param value the value to encode
     * @throws IOException
     */
    public void encodeValue(Type type, Object value) throws IOException {
        encodeValue(Types.getVdlTypeFromReflect(type), value);
    }

    /**
     * Encodes a VDL value into binary VOM format.
     *
     * @param value the value to encode
     * @throws IOException
     */
    public void encodeValue(VdlValue value) throws IOException {
        encodeValue(value.vdlType(), value);
    }

    private void writeMessage(ByteArrayOutputStream buffer, long messageId, boolean encodeLength)
            throws IOException {
        BinaryUtil.encodeInt(out, messageId);
        if (encodeLength) {
            BinaryUtil.encodeUint(out, buffer.size());
        }
        buffer.writeTo(out);
    }

    private TypeID getType(VdlType type) throws IOException {
        TypeID typeId = BootstrapType.getBootstrapTypeId(type);
        if (typeId != null) {
            return typeId;
        } else if (visitedTypes.containsKey(type)) {
            return visitedTypes.get(type);
        } else {
            return encodeType(type);
        }
    }

    private TypeID encodeType(VdlType type) throws IOException {
        TypeID typeId = nextTypeId;
        nextTypeId = new TypeID(nextTypeId.getValue() + 1);
        visitedTypes.put(type, typeId);

        VdlValue wireType = new VdlAny(convertToWireType(type));
        typeBuffer.reset();
        writeValue(typeBuffer, wireType, wireType.vdlType());
        writeMessage(typeBuffer, -typeId.getValue(), true);
        return typeId;
    }

    private VdlValue convertToWireType(VdlType type) throws IOException {
        switch (type.getKind()) {
            case BOOL:
            case BYTE:
            case UINT16:
            case UINT32:
            case UINT64:
            case INT16:
            case INT32:
            case INT64:
            case FLOAT32:
            case FLOAT64:
            case COMPLEX64:
            case COMPLEX128:
            case STRING:
                return new WireNamed(type.getName(),
                        getType(Types.primitiveTypeFromKind(type.getKind())));
            case ARRAY:
                return new WireArray(type.getName(), getType(type.getElem()),
                        new TypeID(type.getLength()));
            case ENUM:
                return new WireEnum(type.getName(), type.getLabels());
            case LIST:
                return new WireList(type.getName(), getType(type.getElem()));
            case MAP:
                return new WireMap(type.getName(), getType(type.getKey()), getType(type.getElem()));
            case ONE_OF:
                List<TypeID> typeIds = new ArrayList<TypeID>();
                for (VdlType oneOfType : type.getTypes()) {
                    typeIds.add(getType(oneOfType));
                }
                return new WireOneOf(type.getName(), typeIds);
            case SET:
                return new WireSet(type.getName(), getType(type.getKey()));
            case STRUCT:
                List<WireField> wireFields = new ArrayList<WireField>();
                for (VdlStructField field : type.getFields()) {
                    wireFields.add(new WireField(field.getName(), getType(field.getType())));
                }
                return new WireStruct(type.getName(), wireFields);
            default:
                throw new RuntimeException("Unknown wiretype for kind: " + type.getKind());
        }
    }

    private void writeValue(OutputStream out, Object value, VdlType type) throws IOException {
        switch (type.getKind()) {
            case ANY:
                writeVdlAny(out, value);
                break;
            case ARRAY:
                writeVdlArray(out, value);
                break;
            case BOOL:
                writeVdlBool(out, value);
                break;
            case BYTE:
                writeVdlByte(out, value);
                break;
            case COMPLEX64:
            case COMPLEX128:
                writeVdlComplex(out, value);
                break;
            case ENUM:
                writeVdlEnum(out, value);
                break;
            case FLOAT32:
            case FLOAT64:
                writeVdlFloat(out, value);
                break;
            case INT16:
            case INT32:
            case INT64:
                writeVdlInt(out, value);
                break;
            case LIST:
                writeVdlList(out, value, type);
                break;
            case MAP:
                writeVdlMap(out, value, type);
                break;
            case ONE_OF:
                writeVdlOneOf(out, value);
                break;
            case SET:
                writeVdlSet(out, value, type);
                break;
            case STRING:
                writeVdlString(out, value);
                break;
            case STRUCT:
                writeVdlStruct(out, value);
                break;
            case UINT16:
            case UINT32:
            case UINT64:
                writeVdlUint(out, value);
                break;
            case TYPEOBJECT:
                writeVdlTypeObject(out, value);
                break;
        }
    }

    private void writeVdlAny(OutputStream out, Object value) throws IOException {
        expectClass(Kind.ANY, value, VdlAny.class);
        VdlAny anyValue = (VdlAny) value;
        Object elem = anyValue.getElem();
        if (elem != null) {
            BinaryUtil.encodeUint(out, getType(anyValue.getElemType()).getValue());
            writeValue(out, elem, anyValue.getElemType());
        } else {
            BinaryUtil.encodeUint(out, 0);
        }
    }

    private void writeVdlArray(OutputStream out, Object value) throws IOException {
        expectClass(Kind.ARRAY, value, VdlArray.class);
        VdlArray<?> arrayValue = (VdlArray<?>) value;
        for (Object elem : arrayValue) {
            writeValue(out, elem, arrayValue.vdlType().getElem());
        }
    }

    private void writeVdlBool(OutputStream out, Object value) throws IOException {
        if (value instanceof VdlBool) {
            BinaryUtil.encodeBoolean(out, ((VdlBool) value).getValue());
        } else if (value instanceof Boolean) {
            BinaryUtil.encodeBoolean(out, (Boolean) value);
        } else {
            throw new IOException("Unsupported VDL bool value (type " + value.getClass()
                    + ", value " + value + ")");
        }
    }

    private void writeVdlByte(OutputStream out, Object value) throws IOException {
        if (value instanceof VdlByte) {
            out.write(((VdlByte) value).getValue());
        } else if (value instanceof Byte) {
            out.write((Byte) value);
        } else {
            throw new IOException("Unsupported VDL byte value (type " + value.getClass()
                    + ", value " + value + ")");
        }
    }

    private void writeVdlComplex(OutputStream out, Object value) throws IOException {
        if (value instanceof VdlComplex64) {
            BinaryUtil.encodeDouble(out, ((VdlComplex64) value).getReal());
            BinaryUtil.encodeDouble(out, ((VdlComplex64) value).getImag());
        } else if (value instanceof VdlComplex128) {
            BinaryUtil.encodeDouble(out, ((VdlComplex128) value).getReal());
            BinaryUtil.encodeDouble(out, ((VdlComplex128) value).getImag());
        } else {
            throw new IOException("Unsupported VDL complex value (type " + value.getClass()
                    + ", value " + value + ")");
        }
    }

    private void writeVdlEnum(OutputStream out, Object value) throws IOException {
        expectClass(Kind.ENUM, value, VdlEnum.class);
        VdlEnum enumValue = (VdlEnum) value;
        BinaryUtil.encodeUint(out, enumValue.vdlType().getLabels().indexOf(enumValue.name()));
    }

    private void writeVdlFloat(OutputStream out, Object value) throws IOException {
        if (value instanceof VdlFloat32) {
            BinaryUtil.encodeDouble(out, ((VdlFloat32) value).getValue());
        } else if (value instanceof VdlFloat64) {
            BinaryUtil.encodeDouble(out, ((VdlFloat64) value).getValue());
        } else if (value instanceof Float) {
            BinaryUtil.encodeDouble(out, (Float) value);
        } else if (value instanceof Double){
            BinaryUtil.encodeDouble(out, (Double) value);
        } else {
            throw new IOException("Unsupported VDL float value (type " + value.getClass()
                    + ", value " + value + ")");
        }
    }

    private void writeVdlInt(OutputStream out, Object value) throws IOException {
        if (value instanceof VdlInt16) {
            BinaryUtil.encodeInt(out, ((VdlInt16) value).getValue());
        } else if (value instanceof VdlInt32) {
            BinaryUtil.encodeInt(out, ((VdlInt32) value).getValue());
        } else if (value instanceof VdlInt64) {
            BinaryUtil.encodeInt(out, ((VdlInt64) value).getValue());
        } else if (value instanceof Short){
            BinaryUtil.encodeInt(out, (Short) value);
        } else if (value instanceof Integer) {
            BinaryUtil.encodeInt(out, (Integer) value);
        } else if (value instanceof Long) {
            BinaryUtil.encodeInt(out, (Long) value);
        } else {
            throw new IOException("Unsupported VDL int value (type " + value.getClass()
                    + ", value " + value + ")");
        }
    }

    private void writeVdlList(OutputStream out, Object value, VdlType type) throws IOException {
        if (value instanceof List) {
            List<?> listValue = (List<?>) value;
            BinaryUtil.encodeUint(out, listValue.size());
            for (Object elem : listValue) {
                writeValue(out, elem, type.getElem());
            }
        } else if (value.getClass().isArray()) {
            Object arrayValue = value;
            int len = Array.getLength(arrayValue);
            BinaryUtil.encodeUint(out, len);
            for (int i = 0; i < len; i++) {
                writeValue(out, Array.get(arrayValue, i), type.getElem());
            }
        } else {
            throw new IOException("Unsupported VDL list value (type " + value.getClass()
                    + ", value " + value + ")");
        }
    }

    private void writeVdlMap(OutputStream out, Object value, VdlType type) throws IOException {
        expectClass(Kind.MAP, value, Map.class);
        Map<?, ?> mapValue = (Map<?, ?>) value;
        BinaryUtil.encodeUint(out, mapValue.size());
        for (Map.Entry<?, ?> entry : mapValue.entrySet()) {
            writeValue(out, entry.getKey(), type.getKey());
            writeValue(out, entry.getValue(), type.getElem());
        }
    }

    private void writeVdlOneOf(OutputStream out, Object value) throws IOException {
        expectClass(Kind.ONE_OF, value, VdlOneOf.class);
        VdlOneOf oneOfValue = (VdlOneOf) value;
        Object elem = oneOfValue.getElem();
        if (elem != null) {
            BinaryUtil.encodeUint(out, getType(oneOfValue.getElemType()).getValue());
            writeValue(out, elem, oneOfValue.getElemType());
        } else {
            BinaryUtil.encodeUint(out, 0);
        }
    }

    private void writeVdlSet(OutputStream out, Object value, VdlType type) throws IOException {
        expectClass(Kind.SET, value, Set.class);
        Set<?> setValue = (Set<?>) value;
        BinaryUtil.encodeUint(out, setValue.size());
        for (Object key : setValue) {
            writeValue(out, key, type.getKey());
        }
    }

    private void writeVdlString(OutputStream out, Object value) throws IOException {
        String stringValue;
        if (value instanceof VdlString) {
            stringValue = ((VdlString) value).getValue();
        } else if (value instanceof String ){
            stringValue = (String) value;
        } else {
            throw new IOException("Unsupported VDL string value (type " + value.getClass()
                    + ", value " + value + ")");
        }
        BinaryUtil.encodeBytes(out, BinaryUtil.getBytes(stringValue));
    }

    private void writeVdlStruct(OutputStream out, Object value) throws IOException {
        expectClass(Kind.STRUCT, value, AbstractVdlStruct.class);
        List<VdlStructField> fields = ((AbstractVdlStruct) value).vdlType().getFields();
        for (int i = 0; i < fields.size(); i++) {
            VdlStructField field = fields.get(i);
            Object fieldValue = null;
            if (value instanceof VdlStruct) {
                fieldValue = ((VdlStruct) value).getField(field.getName());
            } else {
                try {
                    fieldValue = value.getClass().getMethod("get" + field.getName()).invoke(value);
                } catch (Exception e) {
                    throw new IOException("Unsupported VDL struct value (type " + value.getClass()
                            + ", value " + value + ")");
                }
            }
            if (fieldValue != null) {
                BinaryUtil.encodeUint(out, i + 1);
                writeValue(out, fieldValue, field.getType());
            }
        }
        BinaryUtil.encodeUint(out, 0);
    }

    private void writeVdlUint(OutputStream out, Object value) throws IOException {
        if (value instanceof VdlUint16) {
            BinaryUtil.encodeUint(out, ((VdlUint16) value).getValue());
        } else if (value instanceof VdlUint32) {
            BinaryUtil.encodeUint(out, ((VdlUint32) value).getValue());
        } else if (value instanceof VdlUint64) {
            BinaryUtil.encodeUint(out, ((VdlUint64) value).getValue());
        } else {
            throw new IOException("Unsupported VDL uint value (type " + value.getClass()
                    + ", value " + value + ")");
        }
    }

    private void writeVdlTypeObject(OutputStream out, Object object) throws IOException {
        expectClass(Kind.TYPEOBJECT, object, VdlTypeObject.class);
        VdlTypeObject value = (VdlTypeObject) object;
        BinaryUtil.encodeUint(out, getType(value.getTypeObject()).getValue());
    }

    private void expectClass(Kind kind, Object value, Class<?> klass) throws IOException {
        if (!klass.isAssignableFrom(value.getClass())) {
            throw new IOException("Unsupported VDL " + kind + " value (type " + value.getClass()
                    + ", value " + value + ")");
        }
    }
}
