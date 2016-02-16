// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit.bind;

import android.content.Context;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import java.util.Objects;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

public class TextViewAdapter extends LayoutAdapter<Object, TextViewAdapter.ViewHolder> {
    @Accessors(prefix = "m")
    @Getter
    @RequiredArgsConstructor
    public static class ViewHolder implements io.v.baku.toolkit.bind.ViewHolder {
        private final View mView;
        private final TextView mTextView;

        public ViewHolder(final View itemView, @IdRes final int textFieldId) {
            mView = itemView;
            try {
                mTextView = (TextView) itemView.findViewById(textFieldId);
            } catch (final ClassCastException e) {
                throw new IllegalArgumentException(
                        "TextViewAdapter.ViewHolder requires the resource ID to be a TextView", e);
            }
        }

        public ViewHolder(final TextView view) {
            mView = mTextView = view;
        }

        public void setText(final CharSequence text) {
            mTextView.setText(text);
        }
    }

    @IdRes
    private final int mFieldId;

    public TextViewAdapter(final Context context) {
        this(context, android.R.layout.simple_list_item_1, android.R.id.text1);
    }

    public TextViewAdapter(final Context context, final @LayoutRes int resource) {
        this(context, resource, 0);
    }

    public TextViewAdapter(final Context context, final @LayoutRes int resource,
                           final @IdRes int textViewResourceId) {
        this(LayoutInflater.from(context), resource, textViewResourceId);
    }

    public TextViewAdapter(final LayoutInflater inflater, final @LayoutRes int resource,
                           final @IdRes int textViewResourceId) {
        super(inflater, resource);
        mFieldId = textViewResourceId;
    }

    @Override
    public ViewHolder createViewHolder(final View view) {
        if (mFieldId == 0) {
            try {
                return new ViewHolder((TextView) view);
            } catch (final ClassCastException e) {
                throw new IllegalStateException("TextViewAdapter requires the resource ID to be " +
                        "a TextView or a nonzero field ID for a text view within the resource");
            }
        } else {
            return new ViewHolder(view, mFieldId);
        }
    }

    @Override
    public void bindViewHolder(final ViewHolder viewHolder, final int position,
                               final Object value) {
        viewHolder.setText(format(position, value));
    }

    /**
     * The default implementation passes through the value unaltered if it is a {@link CharSequence}
     * or stringizes otherwise. We avoid agnostic stringization to preserve any Android formatting
     * that might be present, like with {@link android.text.SpannableString}.
     */
    protected CharSequence format(final int position, final Object value) {
        return value instanceof CharSequence ? (CharSequence) value : Objects.toString(value);
    }
}
