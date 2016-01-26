// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.impl.google.lib.discovery.ble;

import junit.framework.TestCase;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import io.v.v23.V;
import io.v.v23.vom.VomUtil;
import io.v.v23.verror.VException;

import io.v.x.ref.lib.discovery.Advertisement;
import io.v.x.ref.lib.discovery.plugins.ble.testdata.AdvertisementConversionTestCase;
import io.v.x.ref.lib.discovery.plugins.ble.testdata.Constants;

/**
 * Tests for {@link BleAdvertisementConverter}
 */
public class BleAdvertisementConverterTest extends TestCase {
    private static Map<UUID, byte[]> convertBAdv(Map<String, byte[]> in) {
        Map<UUID, byte[]> res = new HashMap<>(in.size());
        for (Map.Entry<String, byte[]> entry : in.entrySet()) {
            res.put(UUID.fromString(entry.getKey()), entry.getValue());
        }
        return res;
    }

    public void testConversionToBle() throws IOException {
        // V.init() sets up the jni bindings.
        V.init();
        for (AdvertisementConversionTestCase test : Constants.CONVERSION_TEST_DATA) {
            Map<UUID, byte[]> res = BleAdvertisementConverter.vAdvertismentToBleAttr(
                test.getAdvertisement());
            Map<String, byte[]> want = test.getBleAdvertisement();
            assertEquals(want.size(), res.size());
            for (Map.Entry<UUID, byte[]> entry : res.entrySet()) {
                String stringKey = entry.getKey().toString();
                assertTrue(stringKey + " not matched",
                           Arrays.equals(want.get(stringKey), entry.getValue()));
            }
        }
    }

    public void testConversionFromBle() throws IOException {
        // V.init() sets up the jni bindings.
        V.init();
        for (AdvertisementConversionTestCase test : Constants.CONVERSION_TEST_DATA) {
            Map<UUID, byte[]> bleAdv = convertBAdv(test.getBleAdvertisement());
            Advertisement res = BleAdvertisementConverter.bleAttrToVAdvertisement(bleAdv);
            // We can't use assertEquals here since we need a deep comparison.
            // We compare them by serializing to vom.
            try {
                byte[] wantVom = VomUtil.encode(test.getAdvertisement(), test.getAdvertisement().VDL_TYPE);
                byte[] resVom = VomUtil.encode(res, res.VDL_TYPE);
                assertTrue("expected:<" + test.getAdvertisement() + "> but was:<" + res + ">",
                           Arrays.equals(wantVom, resVom));
            } catch (VException e) {
              fail(e.toString());
            }
        }
    }
}
