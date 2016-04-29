// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android;

import android.content.Context;
import android.content.SharedPreferences;

import io.v.android.error.ErrorReporter;
import io.v.android.v23.V;
import io.v.v23.Options;
import io.v.v23.android.R;
import io.v.v23.context.VContext;
import lombok.Getter;

/**
 * Default implementation of {@link VAndroidContext}. This should be hooked into the lifecycle
 * callbacks of its Android context.
 */
public class ManagedVAndroidContext<T extends Context> extends AbstractVAndroidContext<T> {
    @Getter
    private final T mAndroidContext;
    @Getter
    private final ErrorReporter mErrorReporter;
    @Getter
    private final SharedPreferences mVanadiumPreferences;
    @Getter
    private final VContext mVContext;

    public Options getSavedOptions() {
        return VOptionPreferenceUtils.getOptionsFromPreferences(mVanadiumPreferences);
    }

    private VContext vinit() {
        try {
            return V.init(mAndroidContext, getSavedOptions());
        } catch (final RuntimeException e) {
            if (mVanadiumPreferences.getAll().isEmpty()) {
                throw e;
            } else {
                mErrorReporter.onError(R.string.err_vinit_options, e);
                // Don't actually clear/fix options here; leave that to the user
                return V.init(mAndroidContext);
            }
        }
    }

    public ManagedVAndroidContext(final T androidContext, final ErrorReporter errorReporter) {
        mAndroidContext = androidContext;
        mErrorReporter = errorReporter;

        mVanadiumPreferences = VAndroidContexts.getVanadiumPreferences(mAndroidContext);
        mVContext = vinit();
    }
}
