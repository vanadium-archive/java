// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit.blessings;

import android.content.Intent;

import org.junit.Rule;
import org.junit.Test;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.util.FragmentTestUtil;

import java.util.concurrent.TimeUnit;

import io.v.android.v23.services.blessing.BlessingService;
import io.v.baku.toolkit.RobolectricTestCase;
import io.v.rx.RxTestCase;
import io.v.v23.security.Blessings;
import rx.android.schedulers.AndroidSchedulers;
import rx.util.async.Async;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "android.*"})
@SuppressStaticInitializationFor("io.v.baku.toolkit.blessings.BlessingsUtils")
@PrepareForTest({Blessings.class, BlessingService.class, BlessingsUtils.class})
public class AccountManagerBlessingsFragmentTest extends RobolectricTestCase {
    private static final int MOCK_RESULT_CODE = 42;
    private static final Intent
            BLESSING_REQUEST = mock(Intent.class),
            BLESSING_RESULT = mock(Intent.class);

    /**
     * We have to mock this fragment by extension rather than a Mockito spy because PowerMock won't
     * let Mockito properly mock methods inherited from android classes pulled in by Robolectric.
     */
    public static class MockFragment extends AccountManagerBlessingsFragment {
        @Override
        public void startActivityForResult(final Intent intent, final int requestCode) {
            assertEquals(BLESSING_REQUEST, intent);
            Async.start(() -> {
                onActivityResult(requestCode, MOCK_RESULT_CODE, BLESSING_RESULT);
                return null;
            }, AndroidSchedulers.mainThread());
        }
    }

    @Rule
    public final PowerMockRule rule = new PowerMockRule();

    @Test
    public void test() throws Exception {
        mockStatic(BlessingService.class);
        when(BlessingService.newBlessingIntent(any())).thenReturn(BLESSING_REQUEST);

        // Would ideally be private static final but we need PowerMockito to mock the final
        // Blessings class and since we're running with Robolectric, PowerMock is bound on the
        // instance rather than the class.
        final Blessings MOCK_BLESSINGS = PowerMockito.mock(Blessings.class);

        mockStatic(BlessingsUtils.class);
        when(BlessingsUtils.fromActivityResult(MOCK_RESULT_CODE, BLESSING_RESULT))
                .thenReturn(MOCK_BLESSINGS);

        final MockFragment fragment = new MockFragment();

        FragmentTestUtil.startVisibleFragment(fragment);

        assertEquals(MOCK_BLESSINGS, first(fragment.getRxBlessings()
                .timeout(RxTestCase.BLOCKING_DELAY_MS, TimeUnit.MILLISECONDS)));
    }
}
