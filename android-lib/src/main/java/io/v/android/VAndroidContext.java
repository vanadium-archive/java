// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import io.v.android.error.ErrorReporter;
import io.v.v23.context.VContext;

/**
 * Interface for Vanadium Android {@link Context}s (Activities, Services,
 * Applications, etc.). The default implementation is {@link ManagedVAndroidContext}.
 * <p>
 * Implementations tie together Vanadium and Android lifecycle management. Implementations
 * initialize a Vanadium context on instantiation, presumably during
 * {@link Activity#onCreate(Bundle) onCreate} or similar lifecycle methods. The Vanadium context is
 * then available via {@link #getVContext()}. It is cancelled on {@link #close()}, which should be
 * called from {@link Activity#onDestroy()} or similar lifecycle methods.
 */
public interface VAndroidContext<T extends Context> extends AutoCloseable {
    /**
     * Shared preference key for storing Vanadium options.
     * @see VOptionPreferenceUtils#getOptionsFromPreferences(SharedPreferences)
     */
    String VANADIUM_OPTIONS_SHARED_PREFS = "VanadiumOptions";

    T getAndroidContext();
    // TODO(rosswang): The Baku VAndroidContextTrait had a notion of pluggable BlessingsProvider. We
    // should add pluggable behavior/UI to BlessingsManager or bring back the abstraction.
    ErrorReporter getErrorReporter();
    VContext getVContext();

    /**
     * Cleans up ({@linkplain VContext#cancel() cancels}) the Vanadium context.
     */
    void close();
}
