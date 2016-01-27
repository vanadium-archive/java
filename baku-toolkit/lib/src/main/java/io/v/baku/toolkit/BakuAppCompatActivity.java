// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit;

import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.v7.app.AppCompatActivity;

import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;

/**
 * A default integration with {@link BakuActivityTrait} extending
 * {@link android.support.v7.app.AppCompatActivity}.
 */
@Slf4j
public abstract class BakuAppCompatActivity
        extends VAppCompatActivity implements BakuActivityTrait<AppCompatActivity> {
    @Delegate
    private BakuActivityTrait<AppCompatActivity> mBakuActivityTrait;

    protected BakuActivityTrait<AppCompatActivity> createBakuActivityTrait() {
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
