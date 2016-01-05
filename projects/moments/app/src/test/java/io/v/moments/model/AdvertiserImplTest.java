// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.moments.model;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.v.moments.ifc.Moment;
import io.v.moments.ifc.MomentFactory;
import io.v.moments.lib.Id;
import io.v.moments.lib.V23Manager;
import io.v.v23.context.CancelableVContext;
import io.v.v23.discovery.Attributes;
import io.v.v23.discovery.Service;
import io.v.v23.discovery.VDiscovery;
import io.v.v23.naming.Endpoint;
import io.v.v23.rpc.Server;
import io.v.v23.rpc.ServerStatus;
import io.v.v23.security.BlessingPattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AdvertiserImplTest {
    static final Id ID = Id.makeRandom();
    static final String MOMENT_NAME = "cabbage";
    static final String ADDRESS = "192.168.notrealfoo";

    @Rule
    public ExpectedException mThrown = ExpectedException.none();

    @Mock
    V23Manager mV23Manager;
    @Mock
    MomentFactory mMomentFactory;
    @Mock
    Server mServer;
    @Mock
    Moment mMoment;
    @Mock
    Endpoint mEndpoint;
    @Mock
    ServerStatus mServerStatus;
    @Mock
    CancelableVContext mContext;

    @Captor
    ArgumentCaptor<Service> mAdvertisement;
    @Captor
    ArgumentCaptor<List<BlessingPattern>> mBlessingList;

    Attributes mAttrs;

    AdvertiserImpl mAdvertiser;

    @Before
    public void setup() throws Exception {
        mAttrs = new Attributes(makeFakeAttributes());

        when(mV23Manager.makeServer(
                eq(AdvertiserImpl.NO_MOUNT_NAME),
                any(AdvertiserImpl.MomentServer.class))).thenReturn(mServer);
        when(mServer.getStatus()).thenReturn(mServerStatus);

        Endpoint[] endpoints = {mEndpoint};
        when(mEndpoint.toString()).thenReturn(ADDRESS);

        when(mServerStatus.getEndpoints()).thenReturn(endpoints);

        when(mMomentFactory.makeAttributes(mMoment)).thenReturn(mAttrs);
        when(mMoment.getId()).thenReturn(ID);
        when(mMoment.toString()).thenReturn(MOMENT_NAME);

        List<BlessingPattern> list = any();
        when(mV23Manager.advertise(
                any(Service.class),
                list,
                any(VDiscovery.AdvertiseDoneCallback.class))).thenReturn(mContext);

        mAdvertiser = new AdvertiserImpl(
                mV23Manager, mMomentFactory, mMoment, false);
    }

    @Test
    public void construction1() {
        mThrown.expect(IllegalArgumentException.class);
        mThrown.expectMessage("Null v23Manager");
        mAdvertiser = new AdvertiserImpl(
                null, mMomentFactory, mMoment, false);
    }

    @Test
    public void construction2() {
        mThrown.expect(IllegalArgumentException.class);
        mThrown.expectMessage("Null momentFactoryImpl");
        mAdvertiser = new AdvertiserImpl(
                mV23Manager, null, mMoment, false);
    }

    @Test
    public void construction3() {
        mThrown.expect(IllegalArgumentException.class);
        mThrown.expectMessage("Null moment");
        mAdvertiser = new AdvertiserImpl(
                mV23Manager, mMomentFactory, null, false);
    }

    @Test
    public void advertiseStartSuccess() {
        assertFalse(mAdvertiser.isAdvertising());
        mAdvertiser.advertiseStart();

        verify(mV23Manager).advertise(
                mAdvertisement.capture(),
                mBlessingList.capture(),
                any(VDiscovery.AdvertiseDoneCallback.class));

        assertEquals(0, mBlessingList.getValue().size());

        Service service = mAdvertisement.getValue();
        assertEquals(ID.toString(), service.getInstanceId());
        assertEquals(MOMENT_NAME, service.getInstanceName());
        assertEquals(Config.INTERFACE_NAME, service.getInterfaceName());
        assertEquals(mAttrs, service.getAttrs());

        List<String> addresses = service.getAddrs();
        assertTrue(addresses.contains(ADDRESS));
        assertEquals(1, addresses.size());

        assertTrue(mAdvertiser.isAdvertising());
    }

    @Test
    public void advertiseStartFailure() {
        assertFalse(mAdvertiser.isAdvertising());
        mAdvertiser.advertiseStart();
        assertTrue(mAdvertiser.isAdvertising());
        mThrown.expect(IllegalStateException.class);
        mThrown.expectMessage("Already advertising.");
        mAdvertiser.advertiseStart();
    }

    @Test
    public void advertiseStopFailure() {
        mThrown.expect(IllegalStateException.class);
        mThrown.expectMessage("Not advertising.");
        mAdvertiser.advertiseStop();
    }

    @Test
    public void advertiseStopSuccess() throws Exception {
        advertiseStartSuccess();
        assertTrue(mAdvertiser.isAdvertising());
        mAdvertiser.advertiseStop();
        verify(mContext).cancel();
        verify(mServer).stop();
        assertFalse(mAdvertiser.isAdvertising());
    }

    static Map<String, String> makeFakeAttributes() {
        Map<String, String> result = new HashMap<>();
        result.put("color", "teal");
        return result;
    }
}
