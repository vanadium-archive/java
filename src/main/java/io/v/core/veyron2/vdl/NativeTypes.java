package io.v.core.veyron2.vdl;

import io.v.core.veyron2.verror.VException;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * NativeTypes provides helpers to register conversion of values from Java native types
 * (like org.joda.time) to VDL wire representation.
 */
public class NativeTypes {
    /**
     * @throws IllegalArgumentException if value is not an instance of expected class
     */
    static void assertInstanceOf(Object value, Class<?> expectedClass) {
        if (!expectedClass.isAssignableFrom(value.getClass())) {
            throw new IllegalArgumentException("Unexpected value class: expected "
                    + expectedClass + ", got " + value.getClass());
        }
    }

    /**
     * Converts {@code VException}.
     */
    static final class VExceptionCoverter extends Converter {
        static final VExceptionCoverter INSTANCE = new VExceptionCoverter();

        private VExceptionCoverter() {
            super(WireError.class);
        }

        @Override
        public WireError vdlValueFromNative(Object nativeValue) {
            assertInstanceOf(nativeValue, VException.class);
            final VException e = (VException) nativeValue;
            final List<VdlAny> paramVals = new ArrayList<VdlAny>();
            final Serializable[] params = e.getParams();
            final Type[] paramTypes = e.getParamTypes();
            for (int i = 0; i < params.length; ++i) {
                if (params[i] instanceof VdlValue) {
                    paramVals.add(new VdlAny((VdlValue) params[i]));
                } else {
                    try {
                        final VdlType vdlType = Types.getVdlTypeFromReflect(paramTypes[i]);
                        paramVals.add(new VdlAny(vdlType, params[i]));
                    } catch (IllegalArgumentException ex) {
                        // Do nothing - the param will be dropped.
                    }
                }
            }
            return new WireError(new IDAction(e.getID(),  new VdlUint32(e.getAction().getValue())),
                    e.getMessage(), paramVals);
        }

        @Override
        public VException nativeFromVdlValue(VdlValue value) {
            assertInstanceOf(value, WireError.class);
            final WireError error = (WireError) value;
            final IDAction idActionVal = error.getIDAction();
            final VException.IDAction idAction = new VException.IDAction(idActionVal.getID(),
                    VException.ActionCode.fromValue(idActionVal.getAction().getValue()));

            final List<VdlAny> paramVals = error.getParamList();
            final Serializable[] params = new Serializable[paramVals.size()];
            final Type[] paramTypes = new Type[paramVals.size()];
            for (int i = 0; i < paramVals.size(); ++i) {
                VdlAny paramVal = paramVals.get(i);
                params[i] = paramVal.getElem();
                try {
                    paramTypes[i] = Types.getReflectTypeForVdl(paramVal.getElemType());
                } catch (IllegalArgumentException e) {
                    paramTypes[i] = VdlValue.class;
                }
            }
            return new VException(idAction, error.getMsg(), params, paramTypes);
        }
    }

    /**
     * Converts java native values to VDL wire representation.
     */
    public static abstract class Converter {
        private final Type wireType;

        public Converter(Type wireType) {
            this.wireType = wireType;
        }

        /**
         * Converts a java native value to a VDL value.
         *
         * @throws IllegalArgumentException if the native value has incorrect type
         */
        public abstract VdlValue vdlValueFromNative(Object nativeValue);

        /**
         * Converts a VDL value to a corresponding java native value.
         *
         * @throws IllegalArgumentException if the VDL value has incorrect type
         */
        public abstract Object nativeFromVdlValue(VdlValue value);

        /**
         * Returns VDL wire type corresponding to the java native type.
         */
        public Type getWireType() {
            return wireType;
        }
    }
}
