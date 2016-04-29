// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.error;

import android.content.Context;
import android.support.annotation.StringRes;
import android.util.Log;
import android.widget.Toast;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ToastingErrorReporter extends DedupingErrorReporter {
    private static final String TAG = ErrorReporter.class.getName();

    public static void reportError(final Context context, final @StringRes int summaryStringId,
                                   final Throwable t) {
        Log.e(TAG, context.getString(summaryStringId), t);
        Toast.makeText(context, summaryStringId, Toast.LENGTH_LONG).show();
    }

    private final Context mContext;

    @Override
    protected void reportError(final @StringRes int summaryStringId, final Throwable t) {
        reportError(mContext, summaryStringId, t);
    }
}
