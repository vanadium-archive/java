// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit;

import android.content.Context;
import android.widget.Toast;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import rx.android.schedulers.AndroidSchedulers;
import rx.subjects.PublishSubject;
import rx.subjects.Subject;

@Slf4j
public class ErrorReporter {
    @AllArgsConstructor
    @EqualsAndHashCode
    private static class ErrorEntry {
        private final int summaryStringId;
        private final Throwable error;
    }

    private final Context mContext;
    private final Subject<ErrorEntry, ErrorEntry> mErrors;

    public ErrorReporter(final Context context) {
        mContext = context;

        mErrors = PublishSubject.create();
        mErrors.distinctUntilChanged()
                .onBackpressureBuffer()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(e -> reportError(e.summaryStringId, e.error),
                        t -> reportError(R.string.err_misc, t));
    }

    /**
     * @param summaryStringId string resource ID for the error summary
     */
    public void onError(final int summaryStringId, final Throwable t) {
        mErrors.onNext(new ErrorEntry(summaryStringId, t));
    }

    protected void reportError(final int summaryStringId, final Throwable t) {
        log.error(mContext.getString(summaryStringId), t);
        Toast.makeText(mContext, summaryStringId, Toast.LENGTH_LONG).show();
    }
}
