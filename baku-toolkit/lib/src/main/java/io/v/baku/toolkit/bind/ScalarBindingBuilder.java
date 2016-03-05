// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit.bind;

import android.support.annotation.IdRes;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.google.common.collect.Iterables;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.v.baku.toolkit.BakuActivityTrait;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.CompositeSubscription;

/**
 * Builder class for scalar Syncbase bindings, which bind individual Syncbase data rows to
 * Android widget properties. The Baku Toolkit offers read-only and read/write scalar data
 * bindings. (Write-only bindings are generally no more useful than
 * {@linkplain BakuActivityTrait#getSyncbaseTable() direct database writes}.)
 *
 * Unidirectional read-only bindings are simpler and preferred, but some standard Android
 * widgets like {@link EditText} are more naturally bidirectional. To behave in a reasonable
 * manner, bidirectional bindings with Android widgets require coordination between the read and
 * write directions. The Baku Toolkit provides default coordination policies for reasonable
 * behavior.
 *
 * The {@link #deleteValue(Object)} and {@link #defaultValue(Object)} options are minimally
 * typesafe, but keeping this builder simple is preferable, and the absence of rigorous static
 * type checking here is acceptable.
 */
public class ScalarBindingBuilder<T>
        extends DerivedBuilder<ScalarBindingBuilder<T>, BindingBuilder> {
    private String mKey;
    private boolean mExplicitDefaultValue;
    private T mDeleteValue, mDefaultValue;
    private final List<CoordinatorChain<T>> mCoordinators = new ArrayList<>();

    public ScalarBindingBuilder(final BindingBuilder base) {
        super(base);
    }

    /**
     * Sets the row name for the Syncbase side of the binding.
     */
    public ScalarBindingBuilder<T> key(final String key) {
        mKey = key;
        return this;
    }

    private String getKey(final Object fallback) {
        return mKey == null ? fallback.toString() : mKey;
    }

    /**
     * For bidirectional bindings, this value being input in the widget will trigger a delete of
     * the bound Syncbase row.
     *
     * Note that although this option is generic and attempts to enforce some weak measure of
     * type safety in normal use, type pollution may still result in a
     * {@link ClassCastException} at build time.
     */
    @SuppressWarnings("unchecked")
    public <U extends T> ScalarBindingBuilder<U> deleteValue(final U deleteValue) {
        mDeleteValue = deleteValue;
        return (ScalarBindingBuilder<U>) this;
    }

    private T getDefaultValue(final T fallback) {
        return mExplicitDefaultValue ? mDefaultValue : fallback;
    }

    /**
     * If the Syncbase row is not present, the widget will be bound to this value.
     *
     * Note that although this option is generic and attempts to enforce some weak measure of
     * type safety in normal use, type pollution may still result in a
     * {@link ClassCastException} at build time.
     */
    @SuppressWarnings("unchecked")
    public <U extends T> ScalarBindingBuilder<U> defaultValue(final U defaultValue) {
        mDefaultValue = defaultValue;
        mExplicitDefaultValue = true;
        return (ScalarBindingBuilder<U>) this;
    }

    /**
     * An alias that sets {@link #defaultValue(Object) defaultValue} and
     * {@link #deleteValue(Object) deleteValue} to {@code zeroValue}.
     */
    public <U extends T> ScalarBindingBuilder<U> zeroValue(final U zeroValue) {
        return deleteValue(zeroValue).defaultValue(zeroValue);
    }

    @SafeVarargs
    public final ScalarBindingBuilder<T> coordinators(final CoordinatorChain<T>... coordinators) {
        mCoordinators.clear();
        return chain(coordinators);
    }

    @SafeVarargs
    public final ScalarBindingBuilder<T> chain(final CoordinatorChain<T>... coordinators) {
        Collections.addAll(mCoordinators, coordinators);
        return this;
    }

    public ScalarBindingBuilder<T> coordinators(final Iterable<CoordinatorChain<T>> coordinators) {
        mCoordinators.clear();
        return chain(coordinators);
    }

    public ScalarBindingBuilder<T> chain(final Iterable<CoordinatorChain<T>> coordinators) {
        Iterables.addAll(mCoordinators, coordinators);
        return this;
    }

    /**
     * Constructs a binding from a `String` in Syncbase to the text of a {@link TextView}, only
     * propagating changes from Syncbase to the `TextView` and not in the other direction. This
     * is a simple unidirectional binding that does not involve any coordinators.
     *
     * If {@link BindingBuilder#subscriptionParent(CompositeSubscription) subscriptionParent} is
     * set, this method adds the generated binding to it.
     */
    public ScalarBindingBuilder<T> bindReadOnly(final TextView textView) {
        @SuppressWarnings("unchecked")
        final ScalarBindingBuilder<String> t = (ScalarBindingBuilder<String>) this;

        mBase.subscribe(TextViewBindingTermini.bindRead(textView,
                SyncbaseBindingTermini.bindRead(mBase.mRxTable, getKey(textView.getId()),
                        String.class, t.getDefaultValue(""))
                        .onBackpressureLatest()
                        .observeOn(AndroidSchedulers.mainThread()),
                mBase.mOnError));

        return this;
    }

    /**
     * Binds to the provided view in a read-only direction. This method delegates to
     * {@link #bindReadOnly(TextView)} for {@link TextView}s. Other view types are not yet
     * supported.
     */
    public ScalarBindingBuilder<T> bindReadOnly(final View view) {
        if (view instanceof TextView) {
            return bindReadOnly((TextView) view);
        } else {
            throw new IllegalArgumentException("No read-only binding for view " + view);
        }
    }

    /**
     * Binds to the view identified by {@code viewId} in a read-only direction.
     * @see #bindTo(View)
     */
    public ScalarBindingBuilder<T> bindReadOnly(final @IdRes int viewId) {
        return bindReadOnly(mBase.mActivity.findViewById(viewId));
    }

    /**
     * Constructs a two-way, bidirectional binding between a `String` in Syncbase and the text
     * of a {@link TextView}.
     *
     * Defaults:
     *
     * * {@link #defaultValue(Object) defaultValue}: `""`
     * * {@link #deleteValue(Object) deleteValue}: `null`
     * * {@link #coordinators(Iterable) coordinators}: {@link DeferReadOnWriteCoordinator}, and
     *   ensures that there is a {@link SuppressWriteOnReadCoordinator} somewhere in the chain,
     *   injecting it right above the `TextView` if absent.
     *
     * The coordination policy must end its read pipeline on the Android main thread.
     * @todo(rosswang): provide a Coordinator that coerces this.
     *
     * If {@link BindingBuilder#subscriptionParent(CompositeSubscription) subscriptionParent} is
     * set, this method adds the generated binding to it.
     * @todo(rosswang): produce a ScalarBinding, and allow mutable bindings.
     */
    public ScalarBindingBuilder<T> bindTo(final TextView textView) {
        @SuppressWarnings("unchecked")
        final ScalarBindingBuilder<String> t = (ScalarBindingBuilder<String>) this;

        TwoWayBinding<String> core = SyncbaseBindingTermini.bind(mBase.mRxTable,
                getKey(textView.getId()), String.class, t.getDefaultValue(""),
                (String) mDeleteValue, mBase.mOnError);
        boolean hasSuppressWriteOnRead = false;
        for (final CoordinatorChain<String> c : t.mCoordinators) {
            core = c.apply(core);
            if (core instanceof SuppressWriteOnReadCoordinator) {
                hasSuppressWriteOnRead = true;
            }
        }
        if (mCoordinators.isEmpty()) {
            core = new DeferReadOnWriteCoordinator<>(core);
        }
        if (!hasSuppressWriteOnRead) {
            core = new SuppressWriteOnReadCoordinator<>(core);
        }
        mBase.subscribe(TextViewBindingTermini.bind(textView, core, mBase.mOnError));

        return this;
    }

    /**
     * Binds to the provided view in a default manner. This method delegates to
     * {@link #bindTo(TextView)} for {@link TextView}s. Other view types are not yet supported.
     */
    public ScalarBindingBuilder<T> bindTo(final View view) {
        if (view instanceof TextView) {
            return bindTo((TextView) view);
        } else {
            throw new IllegalArgumentException("No default binding for view " + view);
        }
    }

    /**
     * Binds to the view identified by {@code viewId}.
     * @see #bindTo(View)
     */
    public ScalarBindingBuilder<T> bindTo(final @IdRes int viewId) {
        return bindTo(mBase.mActivity.findViewById(viewId));
    }
}
