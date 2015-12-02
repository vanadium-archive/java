// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit;

import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.v7.app.AppCompatActivity;

import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

/**
 * A default application of {@link VAndroidContextTrait} extending
 * {@link android.support.v7.app.AppCompatActivity}.
 */
@Accessors(prefix = "m")
@Slf4j
public abstract class VAppCompatActivity extends AppCompatActivity implements VAndroidContextMixin {
    @Getter
    private VAndroidContextTrait<AppCompatActivity> mVAndroidContextTrait;

    protected VAndroidContextTrait<AppCompatActivity> createVActivityTrait(
            final Bundle savedInstanceState) {
        return VAndroidContextTrait.withDefaults(this, savedInstanceState);
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
}
