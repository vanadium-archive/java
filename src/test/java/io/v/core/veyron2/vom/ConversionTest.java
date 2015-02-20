package io.v.core.veyron2.vom;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.reflect.TypeToken;

import junit.framework.TestCase;

import io.v.core.veyron2.vdl.VdlAny;
import io.v.core.veyron2.vdl.VdlInt32;
import io.v.core.veyron2.vdl.VdlOptional;
import io.v.core.veyron2.vdl.VdlUint32;
import io.v.core.veyron2.vdl.VdlValue;

import java.lang.reflect.Type;
import java.util.Map;

/**
 * Tests for value conversions.
 */
public class ConversionTest extends TestCase {
    private final ImmutableMultimap<VdlValue, Type> tests =
            ImmutableMultimap.<VdlValue, Type>builder()
            .put(new VdlOptional<VdlValue>(new VdlUint32()), VdlInt32.class)
            .put(new VdlOptional<VdlValue>(new VdlUint32()),
                    new TypeToken<VdlOptional<VdlInt32>>(){}.getType())
            .put(new VdlUint32(), VdlInt32.class)
            .put(new VdlUint32(), new TypeToken<VdlOptional<VdlInt32>>(){}.getType())
            .put(new VdlAny(new VdlUint32()), VdlInt32.class)
            .put(new VdlAny(new VdlUint32()), VdlAny.class)
            .put(new VdlUint32(), VdlAny.class)
            .build();

    public void testConversion() throws Exception {
        for (Map.Entry<VdlValue, Type> test : tests.entries()) {
            final byte[] encoded = TestUtil.hexStringToBytes(TestUtil.encode(test.getKey()));
            final Object decoded = TestUtil.decode(encoded, test.getValue());
            Class<?> targetClass = ReflectUtil.getRawClass(test.getValue());
            assertEquals(targetClass, decoded.getClass());
        }
    }
}
