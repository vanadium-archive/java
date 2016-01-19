// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.moments.model;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import io.v.moments.ifc.Advertiser;
import io.v.moments.ifc.Moment;
import io.v.moments.ifc.MomentFactory;
import io.v.moments.lib.Id;
import io.v.moments.lib.V23Manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AdvertiserFactoryTest {
    static final Id ID0 = Id.makeRandom();
    static final Id ID1 = Id.makeRandom();

    @Rule
    public ExpectedException mThrown = ExpectedException.none();

    @Mock
    V23Manager mV23Manager;
    @Mock
    Moment mMoment;
    AdvertiserFactory mFactory;

    @Before
    public void setup() throws Exception {
        mFactory = new AdvertiserFactory(mV23Manager);
    }

    @Test
    public void makeOne() throws Exception {
        when(mMoment.getId()).thenReturn(ID0);

        Advertiser a0 = mFactory.getOrMake(mMoment);
        assertTrue(mFactory.contains(ID0));

        Iterator<Advertiser> iter = mFactory.allAdvertisers().iterator();
        assertEquals(a0, iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void makeTwo() throws Exception {
        when(mMoment.getId()).thenReturn(ID0);
        Advertiser a0 = mFactory.getOrMake(mMoment);
        when(mMoment.getId()).thenReturn(ID1);
        Advertiser a1 = mFactory.getOrMake(mMoment);

        assertTrue(mFactory.contains(ID0));
        assertTrue(mFactory.contains(ID1));

        Set<Advertiser> set = new HashSet<>();
        for (Advertiser ad : mFactory.allAdvertisers()) {
            set.add(ad);
        }
        assertEquals(2, set.size());
        assertTrue(set.contains(a1));
        assertTrue(set.contains(a0));
    }
}
