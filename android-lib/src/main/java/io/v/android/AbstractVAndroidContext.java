// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android;

import android.content.Context;

public abstract class AbstractVAndroidContext<T extends Context> implements VAndroidContext<T> {
    @Override
    public void close() {
        getVContext().cancel();
    }
}
