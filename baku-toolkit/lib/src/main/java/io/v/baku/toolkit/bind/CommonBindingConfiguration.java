// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit.bind;


import android.app.Activity;
import android.content.Context;

import io.v.baku.toolkit.BakuActivityTrait;
import io.v.baku.toolkit.VAndroidContextTrait;
import io.v.rx.syncbase.RxTable;
import rx.Subscription;
import rx.functions.Action1;
import rx.subscriptions.CompositeSubscription;

/**
 * Specifies the configuration common to all data binding builders.
 */
public interface CommonBindingConfiguration<T extends CommonBindingConfiguration<T>> {
    // Non-chainable methods:

    Context getDefaultViewAdapterContext();

    /**
     * @return the current subscription parent.
     */
    CompositeSubscription getAllBindings();

    Subscription getLastBinding();

    // Chainable methods:

    T viewAdapterContext(final Context context);

    T rxTable(final RxTable rxTable);

    T activity(final Activity activity);

    /**
     * Configures this builder for a given {@link BakuActivityTrait}. The following properties are
     * derived:
     *
     * * {@link #activity(Activity)}
     * * {@link #rxTable(RxTable)}
     * * {@link #subscriptionParent(CompositeSubscription)}
     * * {@link #onError(Action1)}
     */
    T activity(final BakuActivityTrait<?> trait);

    /**
     * Configures this builder for a given {@link VAndroidContextTrait}. The following properties
     * are derived:
     *
     * * {@link #activity(Activity)}
     * * {@link #onError(Action1)}
     */
    T activity(final VAndroidContextTrait<? extends Activity> trait);

    /**
     * Sets the {@link CompositeSubscription} that will determine the lifespan of data bindings
     * created by this builder. When that subscription is unsubscribed, the associated bindings will
     * be terminated.
     */
    T subscriptionParent(final CompositeSubscription subscriptionParent);

    /**
     * Sets the error handler for bindings created by this builder.
     */
    T onError(final Action1<Throwable> onError);
}
