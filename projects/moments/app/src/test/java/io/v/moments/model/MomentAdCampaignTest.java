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
import org.joda.time.DateTime;

import io.v.moments.ifc.Moment;
import io.v.moments.ifc.MomentFactory;
import io.v.moments.lib.Id;
import io.v.v23.context.VContext;
import io.v.v23.discovery.Attributes;
import io.v.v23.rpc.ServerCall;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MomentAdCampaignTest {
    static final Id ID = Id.makeRandom();
    static final String PIZZA = "pizza";
    static final String AUTHOR = "shake a spear";
    static final String CAPTION = "If we pull this off, we'll eat like kings.";
    static final DateTime CREATION_TIME = DateTime.now();

    @Rule
    public ExpectedException mThrown = ExpectedException.none();

    @Mock
    MomentFactory mMomentFactory;
    @Mock
    Moment mMoment;
    @Mock
    Attributes mAttributes;
    @Mock
    VContext mCtx;
    @Mock
    ServerCall mCall;

    MomentAdCampaign mCampaign;

    @Before
    public void setup() throws Exception {
        when(mMoment.getId()).thenReturn(ID);
        when(mMoment.toString()).thenReturn(PIZZA);
        when(mMoment.getCaption()).thenReturn(CAPTION);
        when(mMoment.getAuthor()).thenReturn(AUTHOR);
        when(mMoment.getCreationTime()).thenReturn(CREATION_TIME);
        when(mMomentFactory.toAttributes(mMoment)).thenReturn(mAttributes);
        mCampaign = new MomentAdCampaign(mMoment, mMomentFactory);
    }

    @Test
    public void makeWithoutMomentThrowsException() {
        mThrown.expect(IllegalArgumentException.class);
        mThrown.expectMessage("Null moment");
        mCampaign = new MomentAdCampaign(null, mMomentFactory);
    }

    @Test
    public void makeWithoutFactoryThrowsException() {
        mThrown.expect(IllegalArgumentException.class);
        mThrown.expectMessage("Null factory");
        mCampaign = new MomentAdCampaign(mMoment, null);
    }

    @Test
    public void emptyMountName() throws Exception {
        assertEquals("", mCampaign.getMountName());
    }

    @Test
    public void properInterfaceName() throws Exception {
        assertEquals(
                MomentAdCampaign.INTERFACE_NAME, mCampaign.getInterfaceName());
    }

    @Test
    public void factoryMakesAttributes() throws Exception {
        assertSame(mAttributes, mCampaign.getAttributes());
    }

    /**
     * TODO(jregan): Service needs more coverage.
     */
    @Test
    public void checkService() throws Exception {
        MomentIfcServer server = (MomentIfcServer) mCampaign.makeService();
        assertNotNull(server);
        MomentWireData data = server.getBasics(mCtx, mCall).get();
        assertEquals(AUTHOR, data.getAuthor());
        assertEquals(CAPTION, data.getCaption());
        assertEquals(CREATION_TIME.getMillis(), data.getCreationTime());
    }
}
