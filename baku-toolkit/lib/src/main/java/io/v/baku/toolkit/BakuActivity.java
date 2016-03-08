// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit;

import android.app.Activity;
import android.os.Bundle;
import android.os.PersistableBundle;

import io.v.baku.toolkit.bind.BindingBuilder;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;

/**
 * A default integration with {@link BakuActivityTrait} extending {@link android.app.Activity}. Most
 * activities with distributed state should inherit from this.
 *
 * Example usage:
 *
 * ```java
 * public class SampleBakuActivity extends BakuActivity {
 *     {@literal @}Override
 *     protected void onCreate(final Bundle savedInstanceState) {
 *         super.onCreate(savedInstanceState);
 *         setContentView(R.layout.my_activity_layout);
 *
 *         // Example binding between "myDataRow" in Syncbase and myTextView in my_activity_layout.
 *         {@link #binder() binder}().{@link
 *             BindingBuilder#onKey(java.lang.String)
 *             onKey}("myDataRow")
 *                 .{@link io.v.baku.toolkit.bind.ScalarBindingBuilder#bindTo(int)
 *                 bindTo}(R.id.myTextView);
 *     }
 * }
 * ```
 *
 * @see BakuAppCompatActivity
 */
@Slf4j
public abstract class BakuActivity extends VActivity implements BakuActivityTrait<Activity> {
    @Delegate
    private BakuActivityTrait<Activity> mBakuActivityTrait;

    /**
     * Instantiates the {@link BakuActivityTrait} implementation for this Activity. By default, this
     * uses {@link BakuActivityMixin}.
     */
    protected BakuActivityTrait<Activity> createBakuActivityTrait() {
        return new BakuActivityMixin<>(this);
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
}
