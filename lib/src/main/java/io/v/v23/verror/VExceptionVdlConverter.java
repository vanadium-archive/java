// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

 package io.v.v23.verror;

import io.v.v23.vdl.NativeTypes.Converter;
import io.v.v23.vdl.VdlAny;
import io.v.v23.vdl.VdlType;
import io.v.v23.vdl.VdlValue;
import io.v.v23.vdl.WireError;
import io.v.v23.vdl.WireRetryCode;
import io.v.v23.verror.VException.ActionCode;

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

    private WireRetryCode actionCodeToWire(ActionCode code) {
        switch (code) {
            case NO_RETRY: return WireRetryCode.NoRetry;
            case RETRY_CONNECTION: return WireRetryCode.RetryConnection;
            case RETRY_REFETCH: return WireRetryCode.RetryRefetch;
            case RETRY_BACKOFF: return WireRetryCode.RetryBackoff;
            default: return WireRetryCode.NoRetry;
        }
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
        return new WireError(e.getID(), actionCodeToWire(e.getAction()), e.getMessage(), paramVals);
    }

    @Override
    public VException nativeFromVdlValue(VdlValue value) {
        assertInstanceOf(value, WireError.class);
        final WireError error = (WireError) value;
        final VException.IDAction idAction = new VException.IDAction(error.getId(),
                VException.ActionCode.fromValue(error.getRetryCode().ordinal()));

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