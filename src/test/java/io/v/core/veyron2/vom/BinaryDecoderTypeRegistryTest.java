package io.v.core.veyron2.vom;

import junit.framework.TestCase;

import io.v.core.veyron2.vdl.Types;
import io.v.core.veyron2.vdl.VdlBool;
import io.v.core.veyron2.vdl.VdlType;
import io.v.core.veyron2.vom.testdata.NBool;

/**
 * These tests test that BinaryDecoder automatically registers type generated from VDL.
 */
public class BinaryDecoderTypeRegistryTest extends TestCase {
    public void testGuessType() throws Exception {
        VdlType vdlType = Types.named("v.io/core/veyron2/vom/testdata.NBool", Types.BOOL);
        String encoded = TestUtil.encode(new VdlBool(vdlType, true));
        // Make sure that the class NBool is not loaded yet.
        try {
            Types.getReflectTypeForVdl(vdlType);
            fail("Class NBool is not loaded yet");
        } catch (IllegalArgumentException expected) {
        }
        Object value = TestUtil.decode(TestUtil.hexStringToBytes(encoded));
        assertEquals(NBool.class, value.getClass());
        assertEquals(new NBool(true), value);
    }
}
