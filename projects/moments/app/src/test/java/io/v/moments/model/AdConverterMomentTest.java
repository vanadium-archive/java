// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.moments.model;

import android.os.Handler;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.v.moments.ifc.Moment;
import io.v.moments.ifc.MomentClientFactory;
import io.v.moments.ifc.MomentFactory;
import io.v.moments.lib.Id;
import io.v.moments.lib.ObservedList;
import io.v.moments.v23.ifc.V23Manager;
import io.v.v23.context.VContext;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AdConverterMomentTest {
    static final Id ID = Id.makeRandom();
    static final String ADDRESS0 = "192.168.notrealfoo";
    static final String ADDRESS1 = "192.168.notrealbar";
    static final boolean DO_FULL_SIZE = false;
    static final byte[] MOCK_PHOTO_BYTES = {0, 1};
    static final String[] AD_ARRAY = {ADDRESS0, ADDRESS1};
    static final List<String> ADDRESSES = Arrays.asList(AD_ARRAY);

    @Rule public ExpectedException mThrown = ExpectedException.none();

    @Mock V23Manager mV23Manager;
    @Mock MomentFactory mMomentFactory;
    @Mock Handler mHandler;
    @Captor ArgumentCaptor<Runnable> mHandlerRunnable;
    @Mock io.v.v23.discovery.Advertisement mAdvertisement;
    @Mock io.v.v23.discovery.Attributes mAttributes;
    @Mock MomentClientFactory mClientFactory;
    @Mock Moment mMoment;
    @Mock VContext mContext;
    @Mock MomentIfcClient mClient;
    @Mock ListenableFuture<byte[]> mFutureBytes;
    @Mock ObservedList<Moment> mMomentList;
    @Captor ArgumentCaptor<Integer> mOrdinal;

    Map<Id, Moment> mRemoteMomentCache;
    AdConverterMoment mConverter;

    @Before
    public void setup() throws Exception {
        when(mAdvertisement.getId()).thenReturn(ID.toAdId());
        when(mMoment.getId()).thenReturn(ID);
        when(mAdvertisement.getAddresses()).thenReturn(ADDRESSES);
        when(mAdvertisement.getAttributes()).thenReturn(mAttributes);
        when(mMomentFactory.fromAttributes(eq(ID), anyInt(), eq(mAttributes))).thenReturn(mMoment);
        when(mClientFactory.makeClient(eq("/" + ADDRESS0))).thenReturn(mClient);
        when(mV23Manager.contextWithTimeout(AdConverterMoment.Deadline.THUMB)).thenReturn(mContext);
        when(mClient.getThumbImage(mContext)).thenReturn(mFutureBytes);
        when(mFutureBytes.get()).thenReturn(MOCK_PHOTO_BYTES);

        mRemoteMomentCache = new HashMap<>();
        mConverter =
                new AdConverterMoment(
                        mV23Manager,
                        mMomentFactory,
                        MoreExecutors.newDirectExecutorService(),
                        mHandler,
                        mRemoteMomentCache,
                        mClientFactory,
                        DO_FULL_SIZE);
    }

    @Test
    public void makeWithoutObservedListThrowsException() {
        mThrown.expect(IllegalStateException.class);
        mThrown.expectMessage("Must have a list to modify.");
        mConverter.make(mAdvertisement);
    }

    @Test
    public void makeWithMomentAlreadyInCache() throws Exception {
        mConverter.setList(mMomentList);
        mRemoteMomentCache.put(ID, mMoment);
        when(mAdvertisement.getId()).thenReturn(ID.toAdId());
        assertEquals(mMoment, mConverter.make(mAdvertisement));
    }

    @Test
    public void makeNewMoment() throws Exception {
        mConverter.setList(mMomentList);

        // Make the moment - this is the call being tested.
        assertEquals(mMoment, mConverter.make(mAdvertisement));

        verify(mMomentFactory).fromAttributes(eq(ID), mOrdinal.capture(), eq(mAttributes));

        // The ordinal value supplied to the factory should be one.
        assertEquals(1, mOrdinal.getValue().intValue());

        // A crucial result of processing an advertisement is calling a
        // client to get a photo, and saving it as a REMOTE photo.
        verify(mMoment).setPhoto(Moment.Kind.REMOTE, Moment.Style.THUMB, MOCK_PHOTO_BYTES);
        verify(mHandler).post(mHandlerRunnable.capture());

        // Also, on the *UI* thread, the moment list should see a change,
        // and notify its observers.
        mHandlerRunnable.getValue().run();
        verify(mMomentList).changeById(ID); // notification.
        assertEquals(1, mRemoteMomentCache.size());
        assertEquals(mMoment, mRemoteMomentCache.get(ID));
    }
}
