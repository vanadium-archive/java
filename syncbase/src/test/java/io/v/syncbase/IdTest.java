// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static com.google.common.truth.Truth.assertThat;

public class IdTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void goodEncoding() {
        Id original = new Id("aBlessing", "aName");
        String encoded = original.encode();

        Id decoded = Id.decode(encoded);

        assertThat(decoded.getBlessing()).isEqualTo("aBlessing");
        assertThat(decoded.getName()).isEqualTo("aName");
    }

    @Test
    public void invalidEncoding() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Invalid encoded ID");

        Id.decode("not valid");
    }
}
