// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.vom;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import junit.framework.TestCase;

import io.v.v23.vdl.Types;
import io.v.v23.vdl.VdlAny;
import io.v.v23.vdl.VdlArray;
import io.v.v23.vdl.VdlComplex128;
import io.v.v23.vdl.VdlComplex64;
import io.v.v23.vdl.VdlEnum;
import io.v.v23.vdl.VdlField;
import io.v.v23.vdl.VdlOptional;
import io.v.v23.vdl.VdlType;
import io.v.v23.vdl.VdlTypeObject;
import io.v.v23.vdl.VdlUint16;
import io.v.v23.vdl.VdlUint32;
import io.v.v23.vdl.VdlUint64;
import io.v.v23.vdl.VdlUnion;
import io.v.v23.vdl.VdlValue;
import io.v.v23.vom.testdata.Constants;
import io.v.v23.vom.testdata.NStruct;

import java.util.Map;

public class BinaryEncoderTest extends TestCase {
    private static final Map<VdlType, Object> zeroValues = ImmutableMap.<VdlType, Object>builder()
            .put(Types.ANY, new VdlAny())
            .put(Types.arrayOf(4, Types.INT32),
                    new VdlArray<Integer>(Types.arrayOf(4, Types.INT32), new Integer[]{0, 0, 0, 0}))
            .put(Types.BOOL, false)
            .put(Types.BYTE, (byte) 0)
            .put(Types.COMPLEX128, new VdlComplex128(0, 0))
            .put(Types.COMPLEX64, new VdlComplex64(0, 0))
            .put(Types.enumOf("A", "B", "C"), new VdlEnum(Types.enumOf("A", "B", "C"), "A"))
            .put(Types.FLOAT32, 0f)
            .put(Types.FLOAT64, 0.)
            .put(Types.INT16, (short) 0)
            .put(Types.INT32, 0)
            .put(Types.INT64, 0L)
            .put(Types.listOf(Types.INT32), ImmutableList.of())
            .put(Types.mapOf(Types.INT32, Types.INT32), ImmutableMap.of())
            .put(Types.unionOf(new VdlField("A", Types.STRING), new VdlField("B", Types.UINT16)),
                    new VdlUnion(Types.unionOf(new VdlField("A", Types.STRING),
                            new VdlField("B", Types.UINT16)), 0, Types.STRING, ""))
            .put(Types.optionalOf(NStruct.VDL_TYPE), new VdlOptional<VdlValue>(
                    Types.optionalOf(NStruct.VDL_TYPE)))
            .put(Types.setOf(Types.ANY), ImmutableSet.of())
            .put(Types.STRING, "")
            .put(NStruct.VDL_TYPE, new NStruct(false, "", 0L))
            .put(Types.TYPEOBJECT, new VdlTypeObject(Types.ANY))
            .put(Types.UINT16, new VdlUint16((short) 0))
            .put(Types.UINT32, new VdlUint32(0))
            .put(Types.UINT64, new VdlUint64(0L))
            .build();

    public void testEncode() throws Exception {
        for (io.v.v23.vom.testdata.TestCase test : Constants.TESTS) {
            VdlAny value = test.getValue();
            assertEquals(test.getHex(), TestUtil.encode(value.getElemType(), value.getElem()));
        }

        VdlType testsType = Types.getVdlTypeFromReflect(
                Constants.class.getDeclaredField("TESTS").getGenericType());
        assertNotNull(TestUtil.encode(testsType, Constants.TESTS));
    }

    public void testZeroValue() throws Exception {
        for (Map.Entry<VdlType, Object> entry : zeroValues.entrySet()) {
            assertEquals(TestUtil.encode(entry.getKey(), entry.getValue()),
                    TestUtil.encode(entry.getKey(), null));
        }
    }
}
