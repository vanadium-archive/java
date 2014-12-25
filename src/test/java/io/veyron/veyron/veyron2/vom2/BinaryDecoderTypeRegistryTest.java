package io.veyron.veyron.veyron2.vom2;

import junit.framework.TestCase;

import io.veyron.veyron.veyron2.vdl.Types;
import io.veyron.veyron.veyron2.vdl.VdlBool;
import io.veyron.veyron.veyron2.vdl.VdlType;
import io.veyron.veyron.veyron2.vom2.testdata.NBool;

/**
 * These tests test that BinaryDecoder automatically registers type generated from VDL.
 */
public class BinaryDecoderTypeRegistryTest extends TestCase {
    public void testGuessType() throws Exception {
        VdlType vdlType = Types.named("v.io/veyron/veyron2/vom2/testdata.NBool", Types.BOOL);
        String encoded = TestUtil.encode(new VdlBool(vdlType, true));
        // Make sure that the class NBool is not loaded yet.
        assertNull(Types.getReflectTypeForVdl(vdlType));
        Object value = TestUtil.decode(TestUtil.hexStringToBytes(encoded));
        assertEquals(NBool.class, value.getClass());
        assertEquals(new NBool(true), value);
    }
}