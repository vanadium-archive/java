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

import rx.android.schedulers.AndroidSchedulers;

public abstract class SyncbaseBinding {
    /**
     * The {@link #deleteValue(Object)} and {@link #defaultValue(Object)} options are minimally
     * typesafe, but keeping this builder simple is preferable, and the absence of rigorous static
     * type checking here is acceptable.
     */
    public static class Builder<T> extends BaseBuilder<Builder<T>> {
        private String mKey;
        private boolean mExplicitDefaultValue;
        private T mDeleteValue, mDefaultValue;
        private final List<CoordinatorChain<T>> mCoordinators = new ArrayList<>();

        public Builder<T> key(final String key) {
            mKey = key;
            return this;
        }

        private String getKey(final Object fallback) {
            return mKey == null ? fallback.toString() : mKey;
        }

        /**
         * Note that although this option is generic and attempts to enforce some weak measure of
         * type safety in normal use, type pollution may still result in a
         * {@link ClassCastException} at build time.
         */
        @SuppressWarnings("unchecked")
        public <U extends T> Builder<U> deleteValue(final U deleteValue) {
            mDeleteValue = deleteValue;
            return (Builder<U>) this;
        }

        private T getDefaultValue(final T fallback) {
            return mExplicitDefaultValue ? mDefaultValue : fallback;
        }

        /**
         * Note that although this option is generic and attempts to enforce some weak measure of
         * type safety in normal use, type pollution may still result in a
         * {@link ClassCastException} at build time.
         */
        @SuppressWarnings("unchecked")
        public <U extends T> Builder<U> defaultValue(final U defaultValue) {
            mDefaultValue = defaultValue;
            mExplicitDefaultValue = true;
            return (Builder<U>) this;
        }

        public <U extends T> Builder<U> zeroValue(final U zeroValue) {
            return deleteValue(zeroValue).defaultValue(zeroValue);
        }

        @SafeVarargs
        public final Builder<T> coordinators(final CoordinatorChain<T>... coordinators) {
            mCoordinators.clear();
            return chain(coordinators);
        }

        @SafeVarargs
        public final Builder<T> chain(final CoordinatorChain<T>... coordinators) {
            Collections.addAll(mCoordinators, coordinators);
            return this;
        }

        public Builder<T> coordinators(final Iterable<CoordinatorChain<T>> coordinators) {
            mCoordinators.clear();
            return chain(coordinators);
        }

        public Builder<T> chain(final Iterable<CoordinatorChain<T>> coordinators) {
            Iterables.addAll(mCoordinators, coordinators);
            return this;
        }

        public Builder<T> bindOneWay(final TextView textView) {
            @SuppressWarnings("unchecked")
            final Builder<String> t = (Builder<String>) this;

            subscribe(TextViewBindingTermini.bindRead(textView,
                    SyncbaseBindingTermini.bindRead(mRxTable, getKey(textView.getId()),
                            String.class, t.getDefaultValue(""))
                            .onBackpressureLatest()
                            .observeOn(AndroidSchedulers.mainThread()),
                    mOnError));

            return this;
        }

        /**
         * Constructs a two-way binding between a string in Syncbase and the text of a TextView.
         * <p>
         * Defaults:
         * <ul>
         * <li>{@code defaultValue}: {@code ""}</li>
         * <li>{@code deleteValue}: {@code null}</li>
         * <li>{@code coordinators}: {@code DebouncingCoordinator}, and ensures that there is a
         * {@code SuppressWriteOnReadCoordinator} somewhere in the chain, injecting it right
         * above the {@code TextView} if absent.</li>
         * </ul>
         * <p>
         * The coordination policy must end its downlink on the Android main thread.
         * TODO(rosswang): provide a Coordinator that coerces this.
         * <p>
         * If {@code subscriptionParent} is set, this method adds the generated binding to it.
         * <p>
         * TODO(rosswang): produce a SyncbaseBinding, and allow mutable bindings.
         */
        public Builder<T> bindTwoWay(final TextView textView) {
            @SuppressWarnings("unchecked")
            final Builder<String> t = (Builder<String>) this;

            TwoWayBinding<String> core = SyncbaseBindingTermini.bind(mRxTable,
                    getKey(textView.getId()), String.class, t.getDefaultValue(""),
                    (String) mDeleteValue, mOnError);
            boolean hasSuppressWriteOnRead = false;
            for (final CoordinatorChain<String> c : t.mCoordinators) {
                core = c.apply(core);
                if (core instanceof SuppressWriteOnReadCoordinator) {
                    hasSuppressWriteOnRead = true;
                }
            }
            if (mCoordinators.isEmpty()) {
                core = new DebouncingCoordinator<>(core);
            }
            if (!hasSuppressWriteOnRead) {
                core = new SuppressWriteOnReadCoordinator<>(core);
            }
            subscribe(TextViewBindingTermini.bind(textView, core, mOnError));

            return this;
        }

        /**
         * Calls {@link #bindTwoWay(TextView)} if the {@link TextView} is an {@link EditText},
         * {@link #bindOneWay(TextView)} otherwise.
         */
        public Builder<T> bindTo(final TextView textView) {
            return textView instanceof EditText ? bindTwoWay(textView) : bindOneWay(textView);
        }

        /**
         * An alias for {@link #bindTwoWay(TextView)}, which is the default for {@link EditText}
         * widgets.
         */
        public Builder<T> bindTo(final EditText editText) {
            return bindTwoWay(editText);
        }

        public Builder<T> bindTo(final View view) {
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
        public Builder<T> bindTo(final @IdRes int viewId) {
            return bindTo(mActivity.findViewById(viewId));
        }
    }

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }
}
