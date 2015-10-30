// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.impl.google.lib.discovery;

import junit.framework.TestCase;

import java.util.UUID;

import io.v.v23.V;
import io.v.v23.context.VContext;
import io.v.x.ref.lib.discovery.testdata.Constants;
import io.v.x.ref.lib.discovery.testdata.UuidTestData;

/**
 * Tests for {@link UUIDUtil}.
 */
public class UUIDUtilTest extends TestCase {
    public void testInterfaceNameUUID() {
        VContext ctx = V.init();
        for (UuidTestData test: Constants.INTERFACE_NAME_TEST) {
            UUID id = UUIDUtil.UUIDForInterfaceName(test.getIn());
            assertEquals(test.getWant(), id.toString());
        }
    }
}
