// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit;

import android.os.Bundle;
import android.os.PersistableBundle;

import io.v.baku.toolkit.bind.SyncbaseBinding;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

/**
 * A default application of {@link BakuActivityTrait} extending {@link android.app.Activity}. Most
 * activities with distributed state should inherit from this.
 */
@Accessors(prefix = "m")
@Slf4j
public abstract class BakuActivity extends VActivity implements BakuActivityMixin {
    @Getter
    private BakuActivityTrait mBakuActivityTrait;

    protected BakuActivityTrait createBakuActivityTrait() {
        return new BakuActivityTrait(getVAndroidContextTrait());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBakuActivityTrait = createBakuActivityTrait();
    }

    @Override
    public void onCreate(Bundle savedInstanceState, PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);
        mBakuActivityTrait = createBakuActivityTrait();
    }

    @Override
    protected void onDestroy() {
        mBakuActivityTrait.close();
        super.onDestroy();
    }

    public <T> SyncbaseBinding.Builder<T> binder() {
        return getBakuActivityTrait().binder();
    }
}
