package io.v.core.veyron2.vom;

import io.v.core.veyron2.vdl.Kind;
import io.v.core.veyron2.vdl.Types;
import io.v.core.veyron2.vdl.VdlType;
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

    public ConversionTarget(Type targetType, VdlType vdlType, DecodingMode mode) {
        this.targetType = targetType;
        this.targetClass = ReflectUtil.getRawClass(targetType);
        this.vdlType = vdlType;
        this.mode = mode;
    }

    public ConversionTarget(Type targetType) {
        this(targetType, Types.getVdlTypeFromReflect(targetType), DecodingMode.JAVA_OBJECT);
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
