// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit.blessings;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.SettableFuture;

import junit.framework.AssertionFailedError;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;

import io.v.android.libs.security.BlessingsManager;
import io.v.v23.security.Blessings;
import rx.functions.Action1;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

@RunWith(PowerMockRunner.class)
@SuppressStaticInitializationFor({"io.v.android.libs.security.BlessingsManager", "android.app.Fragment"})
@PrepareForTest({Blessings.class, BlessingsManager.class})
public class BlessingsManagerBlessingsProviderTest {
    @Before
    public void setUp() {
        mockStatic(BlessingsManager.class);
    }

    @Test
    public void testColdPassive() {
        new BlessingsManagerBlessingsProvider(null, null)
                .getPassiveRxBlessings()
                .subscribe(b -> fail("Unexpected blessings " + b));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testBlessingsFromManager() throws Exception {
        final Action1<Blessings>
                cold = mock(Action1.class),
                hot = mock(Action1.class);
        final Blessings
                b1 = PowerMockito.mock(Blessings.class),
                b2 = PowerMockito.mock(Blessings.class);

        when(BlessingsManager.getBlessings(any(), any(), any(), anyBoolean()))
                .thenReturn(Futures.immediateFuture(b1), Futures.immediateFuture(b2));

        final RefreshableBlessingsProvider t = new BlessingsManagerBlessingsProvider(null, null);
        t.getPassiveRxBlessings().subscribe(cold);

        t.getRxBlessings().subscribe(hot);
        verify(hot).call(b1);
        verify(cold).call(b1);
        verify(hot, never()).call(b2);

        t.refreshBlessings();
        verify(hot).call(b2);
        verify(cold).call(b2);
    }

    /**
     * Verifies that if a new subscriber needs to seek blessings, the new subscriber does not
     * receive blessings until the seek completes (does not receive old blessings), and that the old
     * subscriber is refreshed as well.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testDeferOnNewSubscriber() throws Exception {
        final Action1<Blessings>
                s1 = mock(Action1.class),
                s2 = mock(Action1.class);
        final Blessings
                b1 = PowerMockito.mock(Blessings.class),
                b2 = PowerMockito.mock(Blessings.class);

        final SettableFuture<Blessings>
                bf1 = SettableFuture.create(),
                bf2 = SettableFuture.create();

        when(BlessingsManager.getBlessings(any(), any(), any(), anyBoolean()))
                .thenReturn(bf1, bf2);

        final RefreshableBlessingsProvider t = new BlessingsManagerBlessingsProvider(null, null);
        t.getRxBlessings().subscribe(s1);
        bf1.set(b1);

        t.getRxBlessings().subscribe(s2);
        verify(s2, never()).call(any());

        bf2.set(b2);
        verify(s1).call(b2);
        verify(s2).call(b2);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testConcurrentSeeks() {
        final Action1<Blessings>
                s1 = mock(Action1.class),
                s2 = mock(Action1.class);
        final Blessings b = PowerMockito.mock(Blessings.class);

        final SettableFuture<Blessings>
                bf = SettableFuture.create();

        when(BlessingsManager.getBlessings(any(), any(), any(), anyBoolean()))
                .thenReturn(bf, Futures.immediateFailedFuture(
                        new AssertionFailedError("Expected at most one getBlessings call.")));

        final RefreshableBlessingsProvider t = new BlessingsManagerBlessingsProvider(null, null);
        t.getRxBlessings().subscribe(s1);
        t.getRxBlessings().subscribe(s2);

        bf.set(b);

        verify(s1).call(b);
        verify(s2).call(b);
    }
}
