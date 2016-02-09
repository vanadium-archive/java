// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit;

import android.app.Activity;
import android.os.Bundle;
import android.os.PersistableBundle;

import lombok.experimental.Delegate;

/**
 * A default integration with {@link VAndroidContextTrait} extending {@link Activity}.
 */
public abstract class VActivity extends Activity implements VAndroidContextTrait<Activity> {
    @Delegate
    private VAndroidContextTrait<Activity> mVAndroidContextTrait;

    protected VAndroidContextTrait<Activity> createVActivityTrait(final Bundle savedInstanceState) {
        return VAndroidContextMixin.withDefaults(this, savedInstanceState);
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mVAndroidContextTrait = createVActivityTrait(savedInstanceState);
    }

    @Override
    public void onCreate(Bundle savedInstanceState, PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);
        mVAndroidContextTrait = createVActivityTrait(savedInstanceState);
    }

    @Override
    protected void onDestroy() {
        mVAndroidContextTrait.close();
        super.onDestroy();
    }
}
