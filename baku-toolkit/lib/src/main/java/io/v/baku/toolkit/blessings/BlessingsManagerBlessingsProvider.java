// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit.blessings;

import android.app.Activity;

import com.google.common.util.concurrent.ListenableFuture;

import io.v.android.libs.security.BlessingsManager;
import io.v.baku.toolkit.VAndroidContextTrait;
import io.v.v23.context.VContext;
import io.v.v23.security.Blessings;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class BlessingsManagerBlessingsProvider extends AbstractRefreshableBlessingsProvider {
    private final VContext mVContext;
    private final Activity mActivity;
    private final String mKey;
    private final boolean mSetAsDefault;

    public BlessingsManagerBlessingsProvider(
            final VAndroidContextTrait<? extends Activity> activity) {
        this(activity.getVContext(), activity.getAndroidContext());
    }

    public BlessingsManagerBlessingsProvider(final VContext vContext, final Activity activity) {
        this(vContext, activity, BlessingsUtils.PREF_BLESSINGS, false);
    }

    @Override
    protected ListenableFuture<Blessings> handleBlessingsRefresh() {
        return BlessingsManager.getBlessings(mVContext, mActivity, mKey, mSetAsDefault);
    }
}
