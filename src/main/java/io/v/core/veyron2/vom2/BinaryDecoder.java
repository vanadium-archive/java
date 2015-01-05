package io.v.core.veyron2.vom2;

import com.google.common.base.Strings;

import io.v.core.veyron2.vdl.GeneratedFromVdl;
import io.v.core.veyron2.vdl.Kind;
import io.v.core.veyron2.vdl.Types;
import io.v.core.veyron2.vdl.VdlAny;
import io.v.core.veyron2.vdl.VdlArray;
import io.v.core.veyron2.vdl.VdlField;
import io.v.core.veyron2.vdl.VdlUnion;
import io.v.core.veyron2.vdl.VdlOptional;
import io.v.core.veyron2.vdl.VdlStruct;
import io.v.core.veyron2.vdl.VdlType;
import io.v.core.veyron2.vdl.VdlType.Builder;
import io.v.core.veyron2.vdl.VdlType.PendingType;
import io.v.core.veyron2.vdl.VdlTypeObject;
import io.v.core.veyron2.vdl.VdlValue;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * BinaryDecoder reads a VDL value from {@code InputStream} encoded in binary VOM format.
 */
public class BinaryDecoder {
    enum DecodingMode {
        JAVA_OBJECT, VDL_VALUE
    }

    private final InputStream in;
    private final Map<TypeID, VdlType> decodedTypes;
    private final Map<TypeID, VdlValue> wireTypes;
    private boolean binaryMagicByteRead;

    public BinaryDecoder(InputStream in) {
        this.in = in;
        this.decodedTypes = new HashMap<TypeID, VdlType>();
        this.wireTypes = new HashMap<TypeID, VdlValue>();
        this.binaryMagicByteRead = false;
    }

    private Object decodeValue(Type targetType, DecodingMode mode) throws IOException,
            ConversionException {
        if (!binaryMagicByteRead) {
            if ((byte) in.read() != BinaryUtil.BINARY_MAGIC_BYTE) {
                throw new CorruptVomStreamException(
                        String.format("The input stream should start with byte %02x",
                        BinaryUtil.BINARY_MAGIC_BYTE));
            }
            binaryMagicByteRead = true;
        }
        VdlType actualType = decodeType();
        assertTypesCompatible(actualType, targetType);
        return readValueMessage(actualType, targetType, mode);
    }

    /**
     * Decodes a VDL value. Returns an instance of provided {@code java.lang.reflect.Type}.
     *
     * @param targetType the type of returned object
     * @return the decoded value
     * @throws IOException
     * @throws ConversionException
     */
    public Object decodeValue(Type targetType) throws IOException, ConversionException {
        if (targetType == VdlValue.class) {
            return decodeValue(targetType, DecodingMode.VDL_VALUE);
        } else {
            return decodeValue(targetType, DecodingMode.JAVA_OBJECT);
        }
    }

    /**
     * Decodes a VDL value.
     * The decoder tries to match named VDL types with Java classes generated from VDL by
     * translating VDL type name to Java class name, initializing class and calling
     * {@code Types.getReflectTypeForVdl}. If the decoder fails to find a matching class for VDL
     * type it will construct a general {@code VdlValue}. Prefer to use {@code decodeValue(Type)}
     * over this method.
     *
     * @return the decoded value
     * @throws IOException
     * @throws ConversionException
     */
    public Object decodeValue() throws IOException, ConversionException {
        return decodeValue(VdlValue.class, DecodingMode.JAVA_OBJECT);
    }

    private void assertTypesCompatible(VdlType actualType, Type targetType)
            throws ConversionException {
        if (targetType != VdlValue.class && !TypeCompatibility.compatible(actualType,
                Types.getVdlTypeFromReflect(targetType))) {
            throw new ConversionException(actualType, targetType, "types are incompatible");
        }
    }

    private Object readValueMessage(VdlType actualType, Type targetType, DecodingMode mode)
            throws IOException, ConversionException {
        if (BinaryUtil.hasBinaryMsgLen(actualType)) {
            // Do nothing with this information for now.
            BinaryUtil.decodeUint(in);
        }
        return readValue(actualType, targetType, mode);
    }

    private VdlType decodeType() throws IOException, ConversionException {
        while (true) {
            long typeId = BinaryUtil.decodeInt(in);
            if (typeId == 0) {
                throw new CorruptVomStreamException("Unexpected zero type ID");
            } else if (typeId > 0) {
                return getType(new TypeID(typeId));
            } else {
                VdlAny wireType = (VdlAny) readValueMessage(
                        Types.ANY, VdlAny.class, DecodingMode.JAVA_OBJECT);
                wireTypes.put(new TypeID(-typeId), (VdlValue) wireType.getElem());
            }
        }
    }

    private VdlType lookupType(TypeID typeId) {
        VdlType type = BootstrapType.getBootstrapType(typeId);
        if (type != null) {
            return type;
        } else if (decodedTypes.containsKey(typeId)) {
            return decodedTypes.get(typeId);
        } else {
            return null;
        }
    }

    private VdlType getType(TypeID typeId) throws CorruptVomStreamException {
        VdlType type = lookupType(typeId);
        if (type != null) {
            return type;
        } else {
            WireToVdlTypeBuilder builder = new WireToVdlTypeBuilder();
            PendingType pendingType = builder.lookupOrBuildPending(typeId);
            builder.build();
            return pendingType.built();
        }
    }

    private Object readValue(VdlType actualType, Type targetType, DecodingMode mode)
            throws IOException, ConversionException {
        ConversionTarget target;
        if (mode == DecodingMode.VDL_VALUE) {
            target = new ConversionTarget(actualType, mode);
        } else if (targetType == VdlValue.class) {
            Type bootstrapClass = Types.getReflectTypeForVdl(actualType);
            if (bootstrapClass != null) {
                target = new ConversionTarget(bootstrapClass);
            } else {
                target = new ConversionTarget(actualType, mode);
            }
        } else {
            target = new ConversionTarget(targetType);
        }

        if (actualType.getKind() != Kind.ANY && actualType.getKind() != Kind.OPTIONAL) {
            if (target.getKind() == Kind.ANY) {
                return new VdlAny((VdlValue) readValue(actualType, VdlValue.class, mode));
            } else if (target.getKind() == Kind.OPTIONAL) {
                return readValue(actualType,
                        ReflectUtil.getElementType(target.getTargetType(), 0), mode);
            }
        }
        switch (actualType.getKind()) {
            case ANY:
                return readVdlAny(target);
            case ARRAY:
            case LIST:
                return readVdlArrayOrVdlList(actualType, target);
            case BOOL:
                return readVdlBool(target);
            case BYTE:
                return readVdlByte(target);
            case COMPLEX64:
            case COMPLEX128:
                return readVdlComplex(target);
            case ENUM:
                return readVdlEnum(actualType, target);
            case FLOAT32:
            case FLOAT64:
                return readVdlFloat(target);
            case INT16:
            case INT32:
            case INT64:
                return readVdlInt(target);
            case MAP:
            case SET:
                return readVdlMapOrSet(actualType, target);
            case STRUCT:
                return readVdlStruct(actualType, target);
            case UNION:
                return readVdlUnion(actualType, target);
            case OPTIONAL:
                return readVdlOptional(actualType, target);
            case STRING:
                return readVdlString(target);
            case TYPEOBJECT:
                return readVdlTypeObject();
            case UINT16:
            case UINT32:
            case UINT64:
                return readVdlUint(target);
            default:
                throw new ConversionException(actualType, targetType);
        }
    }

    private Object createNullValue(ConversionTarget target) throws ConversionException {
        if (target.getKind() == Kind.ANY) {
            return new VdlAny();
        } else if (target.getKind() == Kind.OPTIONAL) {
            return new VdlOptional<VdlValue>(target.getVdlType());
        } else {
            throw new ConversionException("Can't create a null value of " + target.getTargetType());
        }
    }

    private Object readVdlAny(ConversionTarget target) throws IOException, ConversionException {
        TypeID typeId = new TypeID(BinaryUtil.decodeUint(in));
        if (typeId.getValue() == 0) {
            return createNullValue(target);
        }
        Type targetType;
        if (target.getKind() != Kind.ANY) {
            targetType = target.getTargetType();
        } else {
            targetType = VdlValue.class;
        }
        VdlType actualType = getType(typeId);
        assertTypesCompatible(actualType, targetType);
        return new VdlAny(actualType,
                (Serializable) readValue(actualType, targetType, target.getMode()));
    }

    private Object readVdlArrayOrVdlList(VdlType actualType, ConversionTarget target)
            throws IOException, ConversionException {
        int len;
        if (actualType.getKind() == Kind.LIST) {
            len = (int) BinaryUtil.decodeUint(in);
        } else {
            len = actualType.getLength();
        }

        Class<?> targetClass = target.getTargetClass();
        if (!List.class.isAssignableFrom(targetClass) && !targetClass.isArray()) {
            return ConvertUtil.convertFromBytes(BinaryUtil.decodeBytes(in, len), target);
        }

        Type elementType = ReflectUtil.getElementType(target.getTargetType(), 0);
        if (targetClass.isArray() || VdlArray.class.isAssignableFrom(targetClass)) {
            int targetLen = len;
            if (target.getKind() == Kind.ARRAY) {
                if (len > target.getVdlType().getLength()) {
                    throw new ConversionException(actualType, target.getTargetType(),
                            "target array is too short");
                }
                targetLen = target.getVdlType().getLength();
            }
            Class<?> elementClass = ReflectUtil.getRawClass(elementType);
            Object array = Array.newInstance(elementClass, targetLen);
            for (int i = 0; i < len; i++) {
                ReflectUtil.setArrayValue(array, i, readValue(actualType.getElem(), elementType,
                        target.getMode()), elementClass);
            }
            return ReflectUtil.createGeneric(target, array);
        } else {
            List<Object> list = new ArrayList<Object>();
            for (int i = 0; i < len; i++) {
                list.add(readValue(actualType.getElem(), elementType, target.getMode()));
            }
            return ReflectUtil.createGeneric(target, list);
        }
    }

    private Object readVdlBool(ConversionTarget target) throws IOException, ConversionException {
        return ReflectUtil.createPrimitive(target, BinaryUtil.decodeBoolean(in), Boolean.TYPE);
    }

    private Object readVdlByte(ConversionTarget target) throws IOException, ConversionException {
        return ConvertUtil.convertFromByte(BinaryUtil.decodeBytes(in, 1)[0], target);
    }

    private Object readVdlComplex(ConversionTarget target) throws IOException, ConversionException {
        return ConvertUtil.convertFromComplex(BinaryUtil.decodeDouble(in),
                BinaryUtil.decodeDouble(in), target);
    }

    private Object readVdlEnum(VdlType actualType, ConversionTarget target) throws IOException,
            ConversionException {
        int enumIndex = (int) BinaryUtil.decodeUint(in);
        byte[] bytes = actualType.getLabels().get(enumIndex).getBytes(BinaryUtil.UTF8_CHARSET);
        return ConvertUtil.convertFromBytes(bytes, target);
    }

    private Object readVdlFloat(ConversionTarget target) throws IOException, ConversionException {
        return ConvertUtil.convertFromDouble(BinaryUtil.decodeDouble(in), target);
    }

    private Object readVdlInt(ConversionTarget target) throws IOException, ConversionException {
        return ConvertUtil.convertFromInt(BinaryUtil.decodeInt(in), target);
    }

    private Type getMapElemOrStructFieldType(ConversionTarget target, Object key)
            throws ConversionException {
        Class<?> targetClass = target.getTargetClass();
        if (target.getKind() == Kind.MAP) {
            return ReflectUtil.getElementType(target.getTargetType(), 1);
        } else if (target.getKind() == Kind.SET) {
            return Boolean.class;
        } else if (targetClass == VdlStruct.class) {
            return VdlValue.class;
        } else {
            String fieldName = (String) key;
            for (Field field : targetClass.getDeclaredFields()) {
                GeneratedFromVdl annotation = field.getAnnotation(GeneratedFromVdl.class);
                if (annotation != null && annotation.name().equals(fieldName)) {
                    return field.getGenericType();
                }
            }
            return VdlValue.class;
        }
    }

    @SuppressWarnings("unchecked")
    private void setMapElemOrStructField(ConversionTarget target, Object data, Object key,
            Object elem, Type elemType) throws ConversionException {
        if (target.getKind() == Kind.MAP) {
            ((Map<Object, Object>) data).put(key, elem);
        } else if (target.getKind() == Kind.SET) {
            if ((Boolean) elem) {
                ((Set<Object>) data).add(key);
            }
        } else if (data instanceof VdlStruct) {
            ((VdlStruct) data).assignField((String) key, (VdlValue) elem);
        } else {
            if (elemType == VdlValue.class) {
                // no such field, just skip it
                return;
            }
            try {
                data.getClass().getDeclaredMethod("set" + (String) key,
                        ReflectUtil.getRawClass(elemType)).invoke(data, elem);
            } catch (Exception e) {
                throw new ConversionException("Can't set field " + key + " to " + elem + " of "
                        + target.getTargetType() + " : " + e.getMessage());
            }
        }
    }

    private Object createMapOrSetOrStruct(ConversionTarget target) throws ConversionException {
        if (target.getKind() == Kind.MAP) {
            return ReflectUtil.createGeneric(target, new HashMap<Object, Object>());
        } else if (target.getKind() == Kind.SET) {
            return ReflectUtil.createGeneric(target, new HashSet<Object>());
        } else {
            return ReflectUtil.createStruct(target);
        }
    }

    private Type getTargetKeyType(ConversionTarget target) throws ConversionException {
        if (target.getKind() == Kind.MAP || target.getKind() == Kind.SET) {
            return ReflectUtil.getElementType(target.getTargetType(), 0);
        } else {
            return String.class;
        }
    }

    private Object readVdlMapOrSet(VdlType actualType, ConversionTarget target)
            throws IOException, ConversionException {
        Object data = createMapOrSetOrStruct(target);
        Type targetKeyType = getTargetKeyType(target);
        int len = (int) BinaryUtil.decodeUint(in);
        for (int i = 0; i < len; i++) {
            Object key = readValue(actualType.getKey(), targetKeyType, target.getMode());
            Type targetElemType = getMapElemOrStructFieldType(target, key);
            Object elem;
            if (actualType.getKind() == Kind.SET) {
                elem = ReflectUtil.createPrimitive(new ConversionTarget(targetElemType),
                        true, Boolean.TYPE);
            } else {
                elem = readValue(actualType.getElem(), targetElemType, target.getMode());
            }
            setMapElemOrStructField(target, data, key, elem, targetElemType);
        }
        return data;
    }

    private Object readVdlStruct(VdlType actualType, ConversionTarget target)
            throws IOException, ConversionException {
        Object data = createMapOrSetOrStruct(target);
        Type targetKeyType = getTargetKeyType(target);
        while (true) {
            int index = (int) BinaryUtil.decodeUint(in);
            if (index == 0) {
                break;
            }
            VdlField field = actualType.getFields().get(index - 1);
            Type targetElemType = getMapElemOrStructFieldType(target, field.getName());
            Object key = ConvertUtil.convertFromBytes(BinaryUtil.getBytes(field.getName()),
                    new ConversionTarget(targetKeyType));
            Object elem = readValue(field.getType(), targetElemType, target.getMode());
            setMapElemOrStructField(target, data, key, elem, targetElemType);
        }
        return data;
    }

    private Object readVdlUnion(VdlType actualType, ConversionTarget target) throws IOException,
            ConversionException {
        int index = (int) BinaryUtil.decodeUint(in);
        if (index <= 0 || index > actualType.getFields().size()) {
            throw new CorruptVomStreamException("One of index " + index + " is out of range " + 1 +
                    "..." + actualType.getFields().size());
        }
        index--;
        VdlField actualField = actualType.getFields().get(index);
        VdlType actualElemType = actualField.getType();
        // Solve vdl.Value case.
        if (target.getTargetClass() == VdlUnion.class) {
            return new VdlUnion(actualType, index, actualElemType,
                    (VdlValue) readValue(actualElemType, VdlValue.class, target.getMode()));
        }
        Class<?> targetClass = target.getTargetClass();
        // This can happen if targetClass is NamedUnion.A.
        if (targetClass.getSuperclass() != VdlUnion.class) {
            targetClass = targetClass.getSuperclass();
        }
        // Look-up field class in target.
        Class<?> fieldClass = null;
        for (Class<?> klass : targetClass.getDeclaredClasses()) {
            if (klass.getName().equals(targetClass.getName() + "$" + actualField.getName())) {
                fieldClass = klass;
                break;
            }
        }
        if (fieldClass == null) {
            throw new ConversionException(actualType, target.getTargetType());
        }
        try {
            Type elemType = fieldClass.getDeclaredField("elem").getGenericType();
            return fieldClass.getConstructor(ReflectUtil.getRawClass(elemType)).newInstance(
                    readValue(actualElemType, elemType, target.getMode()));
        } catch (Exception e) {
            throw new ConversionException(actualType, target.getTargetType(), e.getMessage());
        }
    }

    private Object readVdlOptional(VdlType actualType, ConversionTarget target) throws IOException,
            ConversionException {
        if (BinaryUtil.decodeUint(in) == 0) {
            return createNullValue(target);
        } else {
            Type type = target.getTargetType();
            if (target.getKind() == Kind.OPTIONAL) {
                type = ReflectUtil.getElementType(target.getTargetType(), 0);
            }
            return new VdlOptional<VdlValue>(
                    (VdlValue) readValue(actualType.getElem(), type, target.getMode()));
        }
    }

    private Object readVdlString(ConversionTarget target) throws IOException, ConversionException {
        int len = (int) BinaryUtil.decodeUint(in);
        byte[] bytes = BinaryUtil.decodeBytes(in, len);
        return ConvertUtil.convertFromBytes(bytes, target);
    }

    private Object readVdlUint(ConversionTarget target) throws IOException, ConversionException {
        return ConvertUtil.convertFromUint(BinaryUtil.decodeUint(in), target);
    }

    private Object readVdlTypeObject() throws IOException {
        return new VdlTypeObject(getType(new TypeID(BinaryUtil.decodeUint(in))));
    }

    /**
     * Builds VdlType from wire type.
     */
    private final class WireToVdlTypeBuilder {
        private final Builder builder;
        private final Map<TypeID, PendingType> pendingTypes;

        public WireToVdlTypeBuilder() {
            builder = new Builder();
            pendingTypes = new HashMap<TypeID, PendingType>();
        }

        public void build() {
            builder.build();
            for (Map.Entry<TypeID, PendingType> entry : pendingTypes.entrySet()) {
                VdlType vdlType = entry.getValue().built();
                if (!Strings.isNullOrEmpty(vdlType.getName())) {
                    Types.loadClassForVdlName(vdlType.getName());
                }
                BinaryDecoder.this.decodedTypes.put(entry.getKey(), vdlType);
            }
        }

        public PendingType lookupOrBuildPending(TypeID typeId) throws CorruptVomStreamException {
            PendingType vdlType = lookupType(typeId);
            if (vdlType != null) {
                return vdlType;
            }
            return buildPendingType(typeId);
        }

        private PendingType lookupType(TypeID typeId) {
            VdlType type = BinaryDecoder.this.lookupType(typeId);
            if (type != null) {
                return builder.builtPendingFromType(type);
            } else if (pendingTypes.containsKey(typeId)) {
                return pendingTypes.get(typeId);
            }
            return null;
        }

        private PendingType buildPendingType(TypeID typeId) throws CorruptVomStreamException {
            VdlValue wireType = BinaryDecoder.this.wireTypes.get(typeId);
            if (wireType == null) {
                throw new CorruptVomStreamException("Unknown wire type " + typeId);
            }
            PendingType pending = builder.newPending();
            pendingTypes.put(typeId, pending);

            if (wireType instanceof WireNamed) {
                WireNamed wireNamed = (WireNamed) wireType;
                return pending.setName(wireNamed.getName())
                        .assignBase(lookupOrBuildPending(wireNamed.getBase()));
            } else if (wireType instanceof WireArray) {
                WireArray wireArray = (WireArray) wireType;
                return pending.setName(wireArray.getName()).setKind(Kind.ARRAY)
                        .setLength((int) wireArray.getLen().getValue())
                        .setElem(lookupOrBuildPending(wireArray.getElem()));
            } else if (wireType instanceof WireEnum) {
                WireEnum wireEnum = (WireEnum) wireType;
                pending.setName(wireEnum.getName()).setKind(Kind.ENUM);
                for (String label : wireEnum.getLabels()) {
                    pending.addLabel(label);
                }
                return pending;
            } else if (wireType instanceof WireList) {
                WireList wireList = (WireList) wireType;
                return pending.setName(wireList.getName()).setKind(Kind.LIST)
                        .setElem(lookupOrBuildPending(wireList.getElem()));
            } else if (wireType instanceof WireMap) {
                WireMap wireMap = (WireMap) wireType;
                return pending.setName(wireMap.getName()).setKind(Kind.MAP)
                        .setKey(lookupOrBuildPending(wireMap.getKey()))
                        .setElem(lookupOrBuildPending(wireMap.getElem()));
            } else if (wireType instanceof WireUnion) {
                WireUnion wireUnion = (WireUnion) wireType;
                pending.setName(wireUnion.getName()).setKind(Kind.UNION);
                for (WireField field : wireUnion.getFields()) {
                    pending.addField(field.getName(), lookupOrBuildPending(field.getType()));
                }
                return pending;
            } else if (wireType instanceof WireSet) {
                WireSet wireSet = (WireSet) wireType;
                return pending.setName(wireSet.getName()).setKind(Kind.SET)
                        .setKey(lookupOrBuildPending(wireSet.getKey()));
            } else if (wireType instanceof WireStruct) {
                WireStruct wireStruct = (WireStruct) wireType;
                pending.setName(wireStruct.getName()).setKind(Kind.STRUCT);
                for (WireField field : wireStruct.getFields()) {
                    pending.addField(field.getName(), lookupOrBuildPending(field.getType()));
                }
                return pending;
            } else if (wireType instanceof WireOptional) {
                WireOptional wireOptional = (WireOptional) wireType;
                return pending.setName(wireOptional.getName()).setKind(Kind.OPTIONAL)
                        .setElem(lookupOrBuildPending(wireOptional.getElem()));
            } else {
                throw new CorruptVomStreamException("Unknown wire type: " + wireType.vdlType());
            }
        }
    }
}
