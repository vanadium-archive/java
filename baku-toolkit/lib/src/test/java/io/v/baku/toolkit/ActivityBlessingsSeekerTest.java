// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit;

import android.app.Activity;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import io.v.android.libs.security.BlessingsManager;
import io.v.v23.security.Blessings;
import rx.functions.Action1;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Blessings.class, BlessingsManager.class})
public class ActivityBlessingsSeekerTest {
    private static class MockActivityBlessingsSeeker extends ActivityBlessingsSeeker {
        public MockActivityBlessingsSeeker(final Activity activity) {
            super(activity, null, false);
        }

        @Override
        protected void seekBlessings() {
        }
    }

    @Before
    public void setUp() {
        mockStatic(BlessingsManager.class);
    }

    @Test
    public void testColdPassive() {
        // would NPE if it tries to do anytyhing
        new MockActivityBlessingsSeeker(null)
                .getPassiveRxBlessings()
                .subscribe(b -> fail("Unexpected blessings " + b));
    }

    @Test
    public void testBlessingsFromManager() throws Exception {
        final Activity activity = mock(Activity.class);
        @SuppressWarnings("unchecked")
        final Action1<Blessings>
                cold = mock(Action1.class),
                hot = mock(Action1.class);
        final Blessings
                b1 = PowerMockito.mock(Blessings.class),
                b2 = PowerMockito.mock(Blessings.class);

        PowerMockito.when(BlessingsManager.getBlessings(any())).thenReturn(b1, b2);

        final MockActivityBlessingsSeeker t = new MockActivityBlessingsSeeker(activity);
        t.getPassiveRxBlessings().subscribe(cold);

        t.getRxBlessings().subscribe(hot);
        verify(hot).call(b1);
        verify(cold).call(b1);
        verify(hot, never()).call(b2);

        t.refreshBlessings();
        verify(hot).call(b2);
        verify(cold).call(b2);
    }

    @Test
    public void testBlessingsFromProvider() throws Exception {
        final Activity activity = mock(Activity.class);
        @SuppressWarnings("unchecked")
        final Action1<Blessings> s = mock(Action1.class);
        final Blessings
                b1 = PowerMockito.mock(Blessings.class),
                b2 = PowerMockito.mock(Blessings.class);

        final MockActivityBlessingsSeeker t = new MockActivityBlessingsSeeker(activity);
        t.getRxBlessings().subscribe(s);
        verify(s, never()).call(any());

        t.setBlessings(b1);
        verify(s).call(b1);

        t.refreshBlessings();
        // The mock BlessingsManager will default to null, so it will seek blessings again.
        t.setBlessings(b2);
        verify(s).call(b2);
    }

    /**
     * Verifies that if a new subscriber needs to seek blessings, the new subscriber does not
     * receive blessings until the seek completes (does not receive old blessings), and that the old
     * subscriber is refreshed as well.
     */
    @Test
    public void testDeferOnNewSubscriber() throws Exception {
        final Activity activity = mock(Activity.class);
        @SuppressWarnings("unchecked")
        final Action1<Blessings>
                s1 = mock(Action1.class),
                s2 = mock(Action1.class);
        final Blessings
                b1 = PowerMockito.mock(Blessings.class),
                b2 = PowerMockito.mock(Blessings.class);

        final MockActivityBlessingsSeeker t = new MockActivityBlessingsSeeker(activity);
        t.getRxBlessings().subscribe(s1);
        t.setBlessings(b1);

        t.getRxBlessings().subscribe(s2);
        verify(s2, never()).call(any());

        t.setBlessings(b2);
        verify(s1).call(b2);
        verify(s2).call(b2);
    }
}
