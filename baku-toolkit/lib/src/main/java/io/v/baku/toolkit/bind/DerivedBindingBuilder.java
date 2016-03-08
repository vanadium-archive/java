// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit.bind;

import android.app.Activity;
import android.content.Context;

import io.v.baku.toolkit.BakuActivityTrait;
import io.v.baku.toolkit.VAndroidContextTrait;
import io.v.rx.syncbase.RxTable;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;
import rx.Subscription;
import rx.functions.Action1;
import rx.subscriptions.CompositeSubscription;

@RequiredArgsConstructor
public class DerivedBindingBuilder<T extends DerivedBindingBuilder<T, B>,
        B extends CommonBindingConfiguration<B>>
        extends ExtensibleBuilder<T>
        implements CommonBindingConfiguration<T> {

    private interface NonChainable {
        Context getDefaultViewAdapterContext();
        CompositeSubscription getAllBindings();
        Subscription getLastBinding();
    }

    @Delegate(types = NonChainable.class)
    protected final B mBase;

    @Override
    public T viewAdapterContext(final Context context) {
        mBase.viewAdapterContext(context);
        return mSelf;
    }

    @Override
    public T activity(final Activity activity) {
        mBase.activity(activity);
        return mSelf;
    }

    @Override
    public T rxTable(final RxTable rxTable) {
        mBase.rxTable(rxTable);
        return mSelf;
    }

    @Override
    public T activity(final BakuActivityTrait<?> trait) {
        mBase.activity(trait);
        return mSelf;
    }

    @Override
    public T activity(final VAndroidContextTrait<? extends Activity> trait) {
        mBase.activity(trait);
        return mSelf;
    }

    @Override
    public T subscriptionParent(final CompositeSubscription subscriptionParent) {
        mBase.subscriptionParent(subscriptionParent);
        return mSelf;
    }

    @Override
    public T onError(final Action1<Throwable> onError) {
        mBase.onError(onError);
        return mSelf;
    }
}
