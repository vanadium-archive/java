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
 * * {@linkplain ScalarBindingBuilder Scalar bindings}, built via the {@link #forKey(String)}
 *   method.
 * * {@linkplain PrefixBindingBuilder Prefix bindings}, built via the {@link #forPrefix(String)}
 *   or {@link #forPrefix(PrefixRange)} method. This is a
 *   {@linkplain CollectionBindingBuilder collection binding}.
 * * {@linkplain IdListBindingBuilder ID-list bindings}, built via the {@link #forIdList(String)}
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
     * Invokes a {@link ScalarBindingBuilder} from the settings configured on this builder.
     */
    protected <T> ScalarBindingBuilder<T> scalarBinder() {
        return new ScalarBindingBuilder<>(this);
    }

    /**
     * Invokes a {@link ScalarBindingBuilder} for the given row name (key).
     */
    public <T> ScalarBindingBuilder<T> forKey(final String key) {
        return this.<T>scalarBinder().key(key);
    }

    /**
     * Invokes a {@link PrefixBindingBuilder} from the settings configured on this builder.
     */
    protected <T, A extends RangeAdapter> PrefixBindingBuilder<T, A> prefixBinder() {
        return new PrefixBindingBuilder<>(this);
    }

    /**
     * Invokes a {@link PrefixBindingBuilder} for the given row name prefix.
     */
    public <T, A extends RangeAdapter> PrefixBindingBuilder<T, A> forPrefix(
            final String prefix) {
        return this.<T, A>prefixBinder().prefix(prefix);
    }

    /**
     * Invokes a {@link PrefixBindingBuilder} for the given row name prefix.
     */
    public <T, A extends RangeAdapter> PrefixBindingBuilder<T, A> forPrefix(
            final PrefixRange prefix) {
        return this.<T, A>prefixBinder().prefix(prefix);
    }

    /**
     * Invokes an {@link IdListBindingBuilder} from the settings configured on this builder.
     */
    protected <A extends RangeAdapter> IdListBindingBuilder<A> idListBinder() {
        return new IdListBindingBuilder<>(this);
    }

    /**
     * Invokes a {@link IdListBindingBuilder} for the given ID list row name.
     */
    public <A extends RangeAdapter> IdListBindingBuilder<A> forIdList(
            final String idListRowName) {
        return this.<A>idListBinder().idListRowName(idListRowName);
    }
}
