package io.v.core.veyron2.vom;

import com.google.common.reflect.TypeToken;

import io.v.core.veyron2.vdl.Kind;
import io.v.core.veyron2.vdl.Types;
import io.v.core.veyron2.vdl.VdlAny;
import io.v.core.veyron2.vdl.VdlArray;
import io.v.core.veyron2.vdl.VdlBool;
import io.v.core.veyron2.vdl.VdlByte;
import io.v.core.veyron2.vdl.VdlComplex128;
import io.v.core.veyron2.vdl.VdlComplex64;
import io.v.core.veyron2.vdl.VdlEnum;
import io.v.core.veyron2.vdl.VdlFloat32;
import io.v.core.veyron2.vdl.VdlFloat64;
import io.v.core.veyron2.vdl.VdlInt16;
import io.v.core.veyron2.vdl.VdlInt32;
import io.v.core.veyron2.vdl.VdlInt64;
import io.v.core.veyron2.vdl.VdlList;
import io.v.core.veyron2.vdl.VdlMap;
import io.v.core.veyron2.vdl.VdlOptional;
import io.v.core.veyron2.vdl.VdlSet;
import io.v.core.veyron2.vdl.VdlString;
import io.v.core.veyron2.vdl.VdlStruct;
import io.v.core.veyron2.vdl.VdlType;
import io.v.core.veyron2.vdl.VdlTypeObject;
import io.v.core.veyron2.vdl.VdlUint16;
import io.v.core.veyron2.vdl.VdlUint32;
import io.v.core.veyron2.vdl.VdlUint64;
import io.v.core.veyron2.vdl.VdlUnion;
import io.v.core.veyron2.vdl.VdlValue;
import io.v.core.veyron2.vom.BinaryDecoder.DecodingMode;

import java.lang.reflect.Type;

/**
 * ConversionTarget keeps type information required for value conversion.
 */
public class ConversionTarget {
    private final Class<?> targetClass;
    private final Type targetType;
    private final VdlType vdlType;
    private final DecodingMode mode;

    private static Type getDefaultReflectType(VdlType type, DecodingMode mode) {
        switch (type.getKind()) {
            case ANY:
                return VdlAny.class;
            case ARRAY:
                return new TypeToken<VdlArray<VdlValue>>(){}.getType();
            case BOOL:
                if (mode == DecodingMode.JAVA_OBJECT) {
                    return Boolean.class;
                } else {
                    return VdlBool.class;
                }
            case BYTE:
                if (mode == DecodingMode.JAVA_OBJECT) {
                    return Byte.class;
                } else {
                    return VdlByte.class;
                }
            case COMPLEX128:
                return VdlComplex128.class;
            case COMPLEX64:
                return VdlComplex64.class;
            case ENUM:
                return VdlEnum.class;
            case FLOAT32:
                if (mode == DecodingMode.JAVA_OBJECT) {
                    return Float.class;
                } else {
                    return VdlFloat32.class;
                }
            case FLOAT64:
                if (mode == DecodingMode.JAVA_OBJECT) {
                    return Double.class;
                } else {
                    return VdlFloat64.class;
                }
            case INT16:
                if (mode == DecodingMode.JAVA_OBJECT) {
                    return Short.class;
                } else {
                    return VdlInt16.class;
                }
            case INT32:
                if (mode == DecodingMode.JAVA_OBJECT) {
                    return Integer.class;
                } else {
                    return VdlInt32.class;
                }
            case INT64:
                if (mode == DecodingMode.JAVA_OBJECT) {
                    return Long.class;
                } else {
                    return VdlInt64.class;
                }
            case LIST:
                return new TypeToken<VdlList<VdlValue>>(){}.getType();
            case MAP:
                return new TypeToken<VdlMap<VdlValue, VdlValue>>(){}.getType();
            case UNION:
                return VdlUnion.class;
            case OPTIONAL:
                return new TypeToken<VdlOptional<VdlValue>>(){}.getType();
            case SET:
                return new TypeToken<VdlSet<VdlValue>>(){}.getType();
            case STRING:
                if (mode == DecodingMode.JAVA_OBJECT) {
                    return String.class;
                } else {
                    return VdlString.class;
                }
            case STRUCT:
                return VdlStruct.class;
            case TYPEOBJECT:
                return VdlTypeObject.class;
            case UINT16:
                return VdlUint16.class;
            case UINT32:
                return VdlUint32.class;
            case UINT64:
                return VdlUint64.class;
            default:
                throw new RuntimeException("Unsupported kind " + type.getKind());
        }
    }

    private ConversionTarget(Type targetType, VdlType vdlType, DecodingMode mode) {
        this.targetType = targetType;
        this.targetClass = ReflectUtil.getRawClass(targetType);
        this.vdlType = vdlType;
        this.mode = mode;
    }

    public ConversionTarget(Type targetType) {
        this(targetType, Types.getVdlTypeFromReflect(targetType), DecodingMode.JAVA_OBJECT);
    }

    public ConversionTarget(VdlType vdlType, DecodingMode mode) {
        this(getDefaultReflectType(vdlType, mode), vdlType, mode);
    }

    public Class<?> getTargetClass() {
        return targetClass;
    }

    public Type getTargetType() {
        return targetType;
    }

    public VdlType getVdlType() {
        return vdlType;
    }

    public Kind getKind() {
        return vdlType.getKind();
    }

    public DecodingMode getMode() {
        return mode;
    }
}
