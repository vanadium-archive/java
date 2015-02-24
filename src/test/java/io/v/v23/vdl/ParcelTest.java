package io.v.v23.vdl;

import android.os.Parcel;
import android.test.AndroidTestCase;

import io.v.v23.vom.testdata.Constants;
import io.v.v23.vom.testdata.TestCase;

import java.lang.reflect.Type;

/**
 * Tests that the VDL types are correctly parceled.
 */
public class ParcelTest extends AndroidTestCase {
    public void testParcelable() {
        for (TestCase test : Constants.TESTS) {
             final Object value = test.getValue().getElem();
             if (!(value instanceof VdlValue)) continue;

            Type type = Types.getReflectTypeForVdl(test.getValue().getElemType());
            if (type == null) {
                type = value.getClass();
            }

            final Parcel parcelW = Parcel.obtain();
            ParcelUtil.writeValue(parcelW, value, type);
            final byte[] b = parcelW.marshall();

            final Parcel parcelR = Parcel.obtain();
            parcelR.unmarshall(b, 0, b.length);
            parcelR.setDataPosition(0);
            final Object copy = ParcelUtil.readValue(parcelR, value.getClass().getClassLoader(), type);
            assertEquals(value, copy);
            assertEquals(value.hashCode(), copy.hashCode());
        }
    }
}
