// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit;

import android.content.Context;

import io.v.baku.toolkit.blessings.BlessingsProvider;
import io.v.v23.context.VContext;

public interface VAndroidContextTrait<T extends Context> {
    String VANADIUM_OPTIONS_SHARED_PREFS = "VanadiumOptions";

    T getAndroidContext();
    BlessingsProvider getBlessingsProvider();
    ErrorReporter getErrorReporter();
    VContext getVContext();
}
