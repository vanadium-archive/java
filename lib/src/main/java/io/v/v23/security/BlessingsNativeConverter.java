// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.security;

import io.v.v23.vdl.NativeTypes;
import io.v.v23.vdl.VdlValue;
import io.v.v23.verror.VException;

/**
 * Converter that translates blessings into their wire representation and vice-versa.
 */
public final class BlessingsNativeConverter extends NativeTypes.Converter {
    public static final BlessingsNativeConverter INSTANCE = new BlessingsNativeConverter();

    private BlessingsNativeConverter() {
        super(WireBlessings.class);
    }

    @Override
    public VdlValue vdlValueFromNative(Object nativeValue) {
        assertInstanceOf(nativeValue, Blessings.class);
        return ((Blessings) nativeValue).wireFormat();
    }

    @Override
    public Object nativeFromVdlValue(VdlValue value) {
        assertInstanceOf(value, WireBlessings.class);
        try {
            return Blessings.create((WireBlessings) value);
        } catch (VException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }
}