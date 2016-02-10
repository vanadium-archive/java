// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.moments.model;

import com.google.common.util.concurrent.FutureCallback;

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
import io.v.moments.lib.Id;
import io.v.moments.lib.V23Manager;
import io.v.v23.context.VContext;
import io.v.v23.discovery.Attributes;
import io.v.v23.discovery.Service;
import io.v.v23.naming.Endpoint;
import io.v.v23.rpc.Server;
import io.v.v23.rpc.ServerStatus;
import io.v.v23.security.BlessingPattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
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
    VContext mServerContext;
    @Mock
    Server mServer;
    @Mock
    Moment mMoment;
    @Mock
    Endpoint mEndpoint;
    @Mock
    ServerStatus mServerStatus;
    @Mock
    VContext mContext;
    @Mock
    FutureCallback<Void> mStartupCallback;
    @Mock
    FutureCallback<Void> mShutdownCallback;
    @Mock
    FutureCallback<Void> mStartupCallback2;
    @Mock
    FutureCallback<Void> mShutdownCallback2;

    @Captor
    ArgumentCaptor<Service> mAdvertisement;
    @Captor
    ArgumentCaptor<List<BlessingPattern>> mBlessingList;
    @Captor
    ArgumentCaptor<FutureCallback<VContext>> mV23StartupCallback;
    @Captor
    ArgumentCaptor<FutureCallback<Void>> mV23ShutdownCallback;
    @Captor
    ArgumentCaptor<Throwable> mThrowable;

    Attributes mAttrs;

    AdvertiserImpl mAdvertiser;

    static Map<String, String> makeFakeAttributes() {
        Map<String, String> result = new HashMap<>();
        result.put("color", "teal");
        return result;
    }

    @Before
    public void setup() throws Exception {
        mAttrs = new Attributes(makeFakeAttributes());

        when(mV23Manager.makeServerContext(
                eq(AdvertiserImpl.NO_MOUNT_NAME),
                any(AdvertiserImpl.MomentServer.class))).thenReturn(mServerContext);
        when(mV23Manager.getServer(mServerContext)).thenReturn(mServer);
        when(mServer.getStatus()).thenReturn(mServerStatus);

        Endpoint[] endpoints = {mEndpoint};
        when(mEndpoint.toString()).thenReturn(ADDRESS);

        when(mServerStatus.getEndpoints()).thenReturn(endpoints);

        when(mMoment.getId()).thenReturn(ID);
        when(mMoment.toString()).thenReturn(MOMENT_NAME);
        when(mMoment.makeAttributes()).thenReturn(mAttrs);

        mAdvertiser = new AdvertiserImpl(mV23Manager, mMoment);
    }

    @Test
    public void construction1() {
        mThrown.expect(IllegalArgumentException.class);
        mThrown.expectMessage("Null v23Manager");
        mAdvertiser = new AdvertiserImpl(null, mMoment);
    }

    @Test
    public void construction2() {
        mThrown.expect(IllegalArgumentException.class);
        mThrown.expectMessage("Null moment");
        mAdvertiser = new AdvertiserImpl(mV23Manager, null);
    }

    @Test
    public void advertiseStartSuccess() {
        assertFalse(mAdvertiser.isAdvertising());
        mAdvertiser.start(mStartupCallback, mShutdownCallback);

        verifyAdvertiseCall();

        Service service = mAdvertisement.getValue();
        assertEquals(ID.toString(), service.getInstanceId());
        assertEquals(MOMENT_NAME, service.getInstanceName());
        assertEquals(Config.Discovery.INTERFACE_NAME, service.getInterfaceName());
        assertSame(mAttrs, service.getAttrs());

        List<String> addresses = service.getAddrs();
        assertTrue(addresses.contains(ADDRESS));
        assertEquals(1, addresses.size());

        assertSame(mV23ShutdownCallback.getValue(), mShutdownCallback);

        assertFalse(mAdvertiser.isAdvertising());
        activateAdvertising();
        assertTrue(mAdvertiser.isAdvertising());
    }

    private void verifyAdvertiseCall() {
        verify(mV23Manager).advertise(
                mAdvertisement.capture(),
                eq(Config.Discovery.DURATION),
                mV23StartupCallback.capture(),
                mV23ShutdownCallback.capture());
    }

    // Make the transition to advertising by returning a context from V23.
    private void activateAdvertising() {
        mV23StartupCallback.getValue().onSuccess(mContext);
        // Verify that the UX will be impacted.
        verify(mStartupCallback).onSuccess(null);
    }

    @Test
    public void advertiseStartFailure() {
        assertFalse(mAdvertiser.isAdvertising());
        mAdvertiser.start(mStartupCallback, mShutdownCallback);
        verifyAdvertiseCall();
        activateAdvertising();
        assertTrue(mAdvertiser.isAdvertising());
        mAdvertiser.start(mStartupCallback2, mShutdownCallback2);
        verify(mStartupCallback2).onFailure(mThrowable.capture());
        assertEquals("Already advertising.", mThrowable.getValue().getMessage());
    }

    @Test
    public void advertiseStopDoesNotThrowIfNotAdvertising() {
        mThrown.expect(IllegalStateException.class);
        mThrown.expectMessage("Not advertising.");
        mAdvertiser.stop();
    }

    @Test
    public void advertiseStopSuccess() throws Exception {
        advertiseStartSuccess();
        assertTrue(mAdvertiser.isAdvertising());
        mAdvertiser.stop();
        verify(mContext).cancel();
        verify(mServerContext).cancel();
        assertFalse(mAdvertiser.isAdvertising());
    }
}
