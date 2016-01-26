// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit.bind;

import android.content.Context;

import lombok.RequiredArgsConstructor;

/**
 * Encapsulates the common logic for building the Syncbase side of a collection binding.
 *
 * @see CollectionAdapterBuilder
 */
@RequiredArgsConstructor
public abstract class BaseCollectionBindingBuilder<B extends BaseCollectionBindingBuilder<B>>
        extends BaseBuilder<B> {
    private Context mViewAdapterContext;

    public B viewAdapterContext(final Context context) {
        mViewAdapterContext = context;
        return mSelf;
    }

    public Context getDefaultViewAdapterContext() {
        return mViewAdapterContext == null ? mActivity : mViewAdapterContext;
    }
}
