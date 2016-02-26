// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.impl.google.lib.discovery;

import java.util.UUID;

import junit.framework.TestCase;

import io.v.v23.V;
import io.v.v23.context.VContext;

import io.v.x.ref.lib.discovery.testdata.Constants;
import io.v.x.ref.lib.discovery.testdata.UuidTestData;

import static com.google.common.truth.Truth.assertThat;

/**
 * Tests for {@link UUIDUtil}.
 */
public class UUIDUtilTest extends TestCase {
    public void testServiceUuidTest() {
        VContext ctx = V.init();
        for (UuidTestData test : Constants.SERVICE_UUID_TEST) {
            UUID uuid = UUIDUtil.serviceUUID(test.getIn());
            assertThat(uuid.toString()).isEqualTo(test.getWant());
        }
    }

    public void testAttributeUuidTest() {
        VContext ctx = V.init();
        for (UuidTestData test : Constants.ATTRIBUTE_UUID_TEST) {
            UUID uuid = UUIDUtil.attributeUUID(test.getIn());
            assertThat(uuid.toString()).isEqualTo(test.getWant());
        }
    }
}
