// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit.bind;


import android.app.Activity;

import io.v.baku.toolkit.BakuActivityTrait;
import io.v.rx.syncbase.RxTable;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import rx.Subscription;
import rx.functions.Action1;
import rx.subscriptions.CompositeSubscription;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public abstract class BaseBuilder<T extends BaseBuilder<T>> {
    @SuppressWarnings("unchecked")
    protected final T mSelf = (T)this;

    protected Activity mActivity;
    protected RxTable mRxTable;
    protected CompositeSubscription mSubscriptionParent;
    protected Action1<Throwable> mOnError;

    public T activity(final Activity activity) {
        mActivity = activity;
        return mSelf;
    }

    public T rxTable(final RxTable rxTable) {
        mRxTable = rxTable;
        return mSelf;
    }

    public T bakuActivity(final BakuActivityTrait<?> trait) {
        return activity(trait.getVAndroidContextTrait().getAndroidContext())
                .rxTable(trait.getSyncbaseTable())
                .subscriptionParent(trait.getSubscriptions())
                .onError(trait::onSyncError);
    }

    public T subscriptionParent(final CompositeSubscription subscriptionParent) {
        mSubscriptionParent = subscriptionParent;
        return mSelf;
    }

    protected Subscription subscribe(final Subscription subscription) {
        if (mSubscriptionParent == null) {
            mSubscriptionParent = new CompositeSubscription();
        }
        mSubscriptionParent.add(subscription);
        return subscription;
    }

    public Subscription getSubscription() {
        return mSubscriptionParent;
    }

    public T onError(final Action1<Throwable> onError) {
        mOnError = onError;
        return mSelf;
    }
}
