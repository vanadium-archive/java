// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.moments.lib;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

@RunWith(MockitoJUnitRunner.class)
public class IdTest {
    @Rule
    public ExpectedException mThrown = ExpectedException.none();

    @Test
    public void equalsMakesSense() {
        Id id1 = Id.makeRandom();
        assertEquals(id1, id1);
    }

    @Test
    public void firstRandomIdsNotEqual() {
        Id id1 = Id.makeRandom();
        Id id2 = Id.makeRandom();
        assertNotEquals(id1, id2);
        assertNotEquals(id1.toLong(), id2.toLong());
    }

    @Test
    public void roundTripsToString() {
        Id id = Id.makeRandom();
        assertEquals(id, Id.fromString(id.toString()));
    }

    @Test
    public void constructionFailure() {
        mThrown.expect(java.lang.IllegalArgumentException.class);
        mThrown.expectMessage("Invalid UUID string: pizza");
        Id id = Id.fromString("pizza");
    }
}
