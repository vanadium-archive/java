 package io.v.core.veyron2.verror;

import io.v.core.veyron2.vdl.IDAction;
import io.v.core.veyron2.vdl.NativeTypes.Converter;
import io.v.core.veyron2.vdl.VdlAny;
import io.v.core.veyron2.vdl.VdlType;
import io.v.core.veyron2.vdl.VdlUint32;
import io.v.core.veyron2.vdl.VdlValue;
import io.v.core.veyron2.vdl.WireError;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Converts {@code VException} to its VDL wire type and vice-versa.
 */
public final class VExceptionVdlConverter extends Converter {
    public static final VExceptionVdlConverter INSTANCE = new VExceptionVdlConverter();

    private VExceptionVdlConverter() {
        super(WireError.class);
    }

    @Override
    public WireError vdlValueFromNative(Object nativeValue) {
        assertInstanceOf(nativeValue, VException.class);
        final VException e = (VException) nativeValue;
        final List<VdlAny> paramVals = new ArrayList<VdlAny>();
        final Serializable[] params = e.getParams();
        final VdlType[] paramTypes = e.getParamTypes();
        for (int i = 0; i < params.length; ++i) {
            if (paramTypes[i] == null) {
                continue;  // dropping the param.
            }
            paramVals.add(new VdlAny(paramTypes[i], params[i]));
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
        final VdlType[] paramTypes = new VdlType[paramVals.size()];
        for (int i = 0; i < paramVals.size(); ++i) {
            final VdlAny paramVal = paramVals.get(i);
            params[i] = paramVal.getElem();
            paramTypes[i] = paramVal.getElemType();
        }
        return new VException(idAction, error.getMsg(), params, paramTypes);
    }
}