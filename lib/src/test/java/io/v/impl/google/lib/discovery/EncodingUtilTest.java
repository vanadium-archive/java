// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.impl.google.lib.discovery;

import junit.framework.TestCase;

import java.io.IOException;

import java.util.Arrays;

import io.v.x.ref.lib.discovery.EncryptionAlgorithm;
import io.v.x.ref.lib.discovery.testdata.PackAddressTest;
import io.v.x.ref.lib.discovery.testdata.Constants;
import io.v.x.ref.lib.discovery.testdata.PackEncryptionKeysTest;

/**
 * Tests for {@link EncodingUtil}.
 */
public class EncodingUtilTest extends TestCase {
    public void testPackAddresses() throws IOException {
        for (PackAddressTest test : Constants.PACK_ADDRESS_TEST_DATA) {
            assertEquals(Arrays.toString(test.getPacked()),
                    Arrays.toString(EncodingUtil.packAddresses(test.getIn())));
        }
    }

    public void testUnpackAddresses() throws IOException {
         for (PackAddressTest test : Constants.PACK_ADDRESS_TEST_DATA) {
            assertEquals(test.getIn(),
                    EncodingUtil.unpackAddresses(test.getPacked()));
        }
    }

    public void testPackEncryptionKeys() throws IOException {
        for (PackEncryptionKeysTest test : Constants.PACK_ENCRYPTION_KEYS_TEST_DATA) {
            byte[] res = EncodingUtil.packEncryptionKeys(test.getAlgo().getValue(),
                    test.getKeys());
            assertEquals(Arrays.toString(test.getPacked()), Arrays.toString(res));
        }
    }

    public void testUnpackEncryptionKeys() throws IOException {
         for (PackEncryptionKeysTest test : Constants.PACK_ENCRYPTION_KEYS_TEST_DATA) {
             EncodingUtil.KeysAndAlgorithm res = EncodingUtil.unpackEncryptionKeys(
                     test.getPacked());
             assertEquals(test.getAlgo(), new EncryptionAlgorithm(res.getEncryptionAlgorithm()));
             assertEquals(test.getKeys(), res.getKeys());
        }
    }
}
