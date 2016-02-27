// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import io.v.baku.toolkit.blessings.BlessingsProvider;
import io.v.v23.context.VContext;

/**
 * [Trait](package-summary.html#mixins) for Vanadium Android {@link Context}s (Activities, Services,
 * Applications, etc.). This trait is implemented by {@link VAndroidContextMixin}.
 *
 * Common Vanadium tasks encapsulated by this trait include:
 *
 * * Vanadium lifecycle management. Implementations initialize a Vanadium context on instantiation,
 *   presumably during {@link Activity#onCreate(Bundle) onCreate}. The Vanadium context is then
 *   available via {@link #getVContext()}. It is cancelled on {@link #close()}.
 * * Blessings management, available via {@link BlessingsProvider#getRxBlessings()
 *   getBlessingsProvider().getRxBlessings()}. Upon `subscribe`, blessings are refreshed from the
 *   {@link BlessingsProvider}. The default `BlessingsProvider` furnished by
 *   {@link VAndroidContextMixin#withDefaults(Activity, Bundle)} is the Vanadium
 *   {@link io.v.android.libs.security.BlessingsManager}.
 */
public interface VAndroidContextTrait<T extends Context> extends AutoCloseable {
    /**
     * Shared preference key for storing Vanadium options.
     * @see VOptionPreferenceUtils#getOptionsFromPreferences(SharedPreferences)
     */
    String VANADIUM_OPTIONS_SHARED_PREFS = "VanadiumOptions";

    T getAndroidContext();
    BlessingsProvider getBlessingsProvider();
    ErrorReporter getErrorReporter();
    VContext getVContext();

    /**
     * Cleans up ({@linkplain VContext#cancel() cancels}) the Vanadium context.
     */
    void close();
}
