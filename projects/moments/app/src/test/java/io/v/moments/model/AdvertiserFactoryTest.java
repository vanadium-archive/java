// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.moments.model;

import org.joda.time.Duration;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import io.v.moments.ifc.Moment;
import io.v.moments.ifc.MomentFactory;
import io.v.moments.lib.Id;
import io.v.moments.v23.ifc.AdCampaign;
import io.v.moments.v23.ifc.Advertiser;
import io.v.moments.v23.ifc.V23Manager;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertSame;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AdvertiserFactoryTest {
    static final Id ID0 = Id.makeRandom();
    static final Id ID1 = Id.makeRandom();

    @Rule
    public ExpectedException mThrown = ExpectedException.none();

    @Captor
    ArgumentCaptor<AdCampaign> mCampaign;
    @Captor
    ArgumentCaptor<Duration> mDuration;

    @Mock
    V23Manager mV23Manager;
    @Mock
    MomentFactory mMomentFactory;
    @Mock
    Moment mMoment;
    @Mock
    Advertiser mAdvertiser0;
    @Mock
    Advertiser mAdvertiser1;

    AdvertiserFactory mFactory;

    @Before
    public void setup() throws Exception {
        mFactory = new AdvertiserFactory(mV23Manager, mMomentFactory);
        when(mV23Manager.makeAdvertiser(
                any(AdCampaign.class))).thenReturn(mAdvertiser0);
    }

    @Test
    public void makeOne() throws Exception {
        when(mMoment.getId()).thenReturn(ID0);

        Advertiser a0 = mFactory.getOrMake(mMoment);
        assertSame(mAdvertiser0, a0);
        assertTrue(mFactory.contains(ID0));

        Iterator<Advertiser> iter = mFactory.allAdvertisers().iterator();
        assertEquals(a0, iter.next());
        assertFalse(iter.hasNext());

        verify(mV23Manager).makeAdvertiser(mCampaign.capture());

        assertNotNull(mCampaign.getValue());
    }

    @Test
    public void makeTwo() throws Exception {
        when(mMoment.getId()).thenReturn(ID0);
        Advertiser a0 = mFactory.getOrMake(mMoment);

        when(mMoment.getId()).thenReturn(ID1);
        when(mV23Manager.makeAdvertiser(
                any(AdCampaign.class))).thenReturn(mAdvertiser1);

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
