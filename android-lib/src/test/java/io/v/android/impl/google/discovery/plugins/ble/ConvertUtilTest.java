// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.impl.google.discovery.plugins.ble;

import junit.framework.TestCase;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import io.v.v23.V;

import io.v.x.ref.lib.discovery.AdInfo;
import io.v.x.ref.lib.discovery.plugins.ble.testdata.AdConversionTestCase;
import io.v.x.ref.lib.discovery.plugins.ble.testdata.Constants;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static io.v.impl.google.lib.discovery.DiscoveryTestUtil.assertThat;

/**
 * Tests for {@link ConvertUtil}
 */
public class ConvertUtilTest extends TestCase {
    protected void setUp() {
        V.init(); // V.init() sets up the jni bindings.
    }

    public void testToGattAttrs() throws IOException {
        for (AdConversionTestCase test : Constants.CONVERSION_TEST_DATA) {
            Map<UUID, byte[]> got = ConvertUtil.toGattAttrs(test.getAdInfo());
            Map<String, byte[]> want = test.getGattAttrs();

            assertThat(got.size()).isEqualTo(want.size());
            for (Map.Entry<UUID, byte[]> entry : got.entrySet()) {
                String uuid = entry.getKey().toString();
                assertWithMessage(uuid).that(entry.getValue()).isEqualTo(want.get(uuid));
            }
        }
    }

    public void testToAdInfo() throws IOException {
        for (AdConversionTestCase test : Constants.CONVERSION_TEST_DATA) {
            Map<UUID, byte[]> attrs = new HashMap<>();
            for (Map.Entry<String, byte[]> entry : test.getGattAttrs().entrySet()) {
                attrs.put(UUID.fromString(entry.getKey()), entry.getValue());
            }

            AdInfo got = ConvertUtil.toAdInfo(attrs);
            assertThat(got).isEqualTo(test.getAdInfo());
        }
    }
}
