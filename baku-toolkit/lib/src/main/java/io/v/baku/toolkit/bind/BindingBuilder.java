// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit.bind;


import android.app.Activity;
import android.content.Context;

import io.v.baku.toolkit.BakuActivityTrait;
import io.v.baku.toolkit.ErrorReporters;
import io.v.baku.toolkit.VAndroidContextTrait;
import io.v.rx.syncbase.RxTable;
import io.v.v23.syncbase.nosql.PrefixRange;
import rx.Subscription;
import rx.functions.Action1;
import rx.subscriptions.CompositeSubscription;

/**
 * Starting builder class for bindings from Syncbase data to UI elements. To build concrete
 * bindings, this builder must be specialized for a specific form of data binding.
 *
 * Three forms of data binding are currently available:
 *
 * * {@linkplain ScalarBindingBuilder Scalar bindings}, built via the {@link #onKey(String)} method.
 * * {@linkplain PrefixBindingBuilder Prefix bindings}, built via the {@link #onPrefix(String)}
 *   or {@link #onPrefix(PrefixRange)} method. This is a
 *   {@linkplain CollectionBindingBuilder collection binding}.
 * * {@linkplain IdListBindingBuilder ID-list bindings}, built via the {@link #onIdList(String)}
 *   method. This is a {@linkplain CollectionBindingBuilder collection binding}.
 */
public class BindingBuilder implements CommonBindingConfiguration<BindingBuilder> {
    protected Activity mActivity;
    protected RxTable mRxTable;
    protected CompositeSubscription mSubscriptionParent;
    protected Subscription mLastSubscription;
    protected Action1<Throwable> mOnError;

    private Context mViewAdapterContext;

    @Override
    public BindingBuilder viewAdapterContext(final Context context) {
        mViewAdapterContext = context;
        return this;
    }

    @Override
    public Context getDefaultViewAdapterContext() {
        return mViewAdapterContext == null ? mActivity : mViewAdapterContext;
    }

    @Override
    public BindingBuilder activity(final Activity activity) {
        mActivity = activity;
        return this;
    }

    @Override
    public BindingBuilder rxTable(final RxTable rxTable) {
        mRxTable = rxTable;
        return this;
    }

    @Override
    public BindingBuilder activity(final BakuActivityTrait<?> trait) {
        return activity(trait.getVAndroidContextTrait().getAndroidContext())
                .rxTable(trait.getSyncbaseTable())
                .subscriptionParent(trait.getSubscriptions())
                .onError(trait::onSyncError);
    }

    @Override
    public BindingBuilder activity(final VAndroidContextTrait<? extends Activity> trait) {
        return activity(trait.getAndroidContext())
                .onError(ErrorReporters.getDefaultSyncErrorReporter(trait));
    }

    @Override
    public BindingBuilder subscriptionParent(final CompositeSubscription subscriptionParent) {
        mSubscriptionParent = subscriptionParent;
        return this;
    }

    protected Subscription subscribe(final Subscription subscription) {
        if (mSubscriptionParent == null) {
            mSubscriptionParent = new CompositeSubscription();
        }
        mSubscriptionParent.add(subscription);
        mLastSubscription = subscription;
        return subscription;
    }

    @Override
    public CompositeSubscription getAllBindings() {
        return mSubscriptionParent;
    }

    @Override
    public Subscription getLastBinding() {
        return mLastSubscription;
    }

    @Override
    public BindingBuilder onError(final Action1<Throwable> onError) {
        mOnError = onError;
        return this;
    }

    /**
     * Invokes a {@link ScalarBindingBuilder} on the given row name (key).
     */
    public <T> ScalarBindingBuilder<T> onKey(final String key) {
        return new ScalarBindingBuilder<T>(this).key(key);
    }

    /**
     * Invokes a {@link PrefixBindingBuilder} on the given row name prefix.
     */
    public <T, A extends RangeAdapter> PrefixBindingBuilder<T, A> onPrefix(
            final String prefix) {
        return new PrefixBindingBuilder<T, A>(this).prefix(prefix);
    }

    /**
     * Invokes a {@link PrefixBindingBuilder} on the given row name prefix.
     */
    public <T, A extends RangeAdapter> PrefixBindingBuilder<T, A> onPrefix(
            final PrefixRange prefix) {
        return new PrefixBindingBuilder<T, A>(this).prefix(prefix);
    }

    /**
     * Invokes a {@link IdListBindingBuilder} on the given ID list row name.
     */
    public <A extends RangeAdapter> IdListBindingBuilder<A> onIdList(
            final String idListRowName) {
        return new IdListBindingBuilder<A>(this).idListRowName(idListRowName);
    }
}
