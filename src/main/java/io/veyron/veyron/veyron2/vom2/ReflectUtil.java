package io.veyron.veyron.veyron2.vom2;

import io.veyron.veyron.veyron2.vdl.VdlComplex128;
import io.veyron.veyron.veyron2.vdl.VdlComplex64;
import io.veyron.veyron.veyron2.vdl.VdlType;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import io.veyron.veyron.veyron2.vdl.VdlValue;


/**
 * ReflectUtil provides helpers to get object properties and create class instances from reflection.
 */
final class ReflectUtil {
    /**
     * Creates an instance of java primitives, one of boolean, byte, short, int, long, float,
     * double, String. Handles java types and VDL types.
     *
     * @param target the target containing java class and VDL type information
     * @param value the value of primitive to be created
     * @return an instance of VDL primitive containing the provided value if the target class
     *         is inherited from {@code VdlValue}; returns provided value otherwise
     * @throws ConversionException if the instance of the target class can't be created
     */
    static Object createPrimitive(ConversionTarget target, Object value,
            Class<?> valueType) throws ConversionException {
        Class<?> targetClass = target.getTargetClass();
        try {
            if (targetClass.getSuperclass() == VdlValue.class) {
                return targetClass.getConstructor(VdlType.class, valueType)
                        .newInstance(target.getVdlType(), value);
            } else if (VdlValue.class.isAssignableFrom(targetClass)) {
                return targetClass.getConstructor(valueType).newInstance(value);
            } else {
                return value;
            }
        } catch (Exception e) {
            throw new ConversionException(
                    "Can't convert " + value + " to " + targetClass + " : " + e.getMessage());
        }
    }

    /**
     * Creates an instance of VDL complex. The target class should be inherited from
     * {@code VdlValue}.
     */
    static VdlValue createComplex(ConversionTarget target, double real, double imag)
            throws ConversionException {
        Class<?> targetClass = target.getTargetClass();
        try {
            if (targetClass == VdlComplex64.class) {
                return new VdlComplex64(target.getVdlType(), (float) real, (float) imag);
            } else if (targetClass == VdlComplex128.class) {
                return new VdlComplex128(target.getVdlType(), real, imag);
            } else if (VdlComplex64.class.isAssignableFrom(targetClass)) {
                return (VdlValue) targetClass.getConstructor(Float.TYPE, Float.TYPE)
                        .newInstance((float) real, (float) imag);
            } else if (VdlComplex128.class.isAssignableFrom(targetClass)) {
                return (VdlValue) targetClass.getConstructor(Double.TYPE, Double.TYPE)
                        .newInstance(real, imag);
            }
        } catch (Exception e) {
            throw new ConversionException("Can't convert " + new VdlComplex128(real, imag) + " to "
                    + targetClass + " : " + e.getMessage());
        }
        throw new ConversionException("Can't convert " + new VdlComplex128(real, imag) + " to "
                + targetClass);
    }

    /**
     * Returns a {@code Class} object that is represented by provided {@code Type} object.
     */
    static Class<?> getRawClass(Type type) {
        if (type instanceof Class) {
            return (Class<?>) type;
        } else if (type instanceof ParameterizedType) {
            return getRawClass(((ParameterizedType) type).getRawType());
        } else if (type instanceof GenericArrayType) {
            Class<?> component = getRawClass(((GenericArrayType) type).getGenericComponentType());
            return Array.newInstance(component, 0).getClass();
        } else {
            return null;
        }
    }
}
