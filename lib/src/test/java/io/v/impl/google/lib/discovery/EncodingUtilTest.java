// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.impl.google.lib.discovery;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import io.v.x.ref.lib.discovery.EncryptionAlgorithm;
import io.v.x.ref.lib.discovery.EncryptionKey;
import io.v.x.ref.lib.discovery.testdata.Constants;
import io.v.x.ref.lib.discovery.testdata.PackEncryptionKeysTest;

import static com.google.common.truth.Truth.assertThat;

/**
 * Tests for {@link EncodingUtil}.
 */
public class EncodingUtilTest extends TestCase {
    public void testPackEncryptionKeys() throws IOException {
        for (PackEncryptionKeysTest test : Constants.PACK_ENCRYPTION_KEYS_TEST_DATA) {
            byte[] packed = EncodingUtil.packEncryptionKeys(test.getAlgo(), test.getKeys());
            assertThat(packed).isEqualTo(test.getPacked());
        }
    }

    public void testUnpackEncryptionKeys() throws IOException {
        for (PackEncryptionKeysTest test : Constants.PACK_ENCRYPTION_KEYS_TEST_DATA) {
            List<EncryptionKey> keys = new ArrayList<>();
            EncryptionAlgorithm algo = EncodingUtil.unpackEncryptionKeys(test.getPacked(), keys);
            assertThat(algo).isEqualTo(test.getAlgo());
            assertThat(keys).isEqualTo(test.getKeys());
        }
    }
}
