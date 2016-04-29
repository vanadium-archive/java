// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.error;

import android.support.annotation.StringRes;

import io.v.v23.android.R;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.subjects.PublishSubject;
import rx.subjects.Subject;

public abstract class DedupingErrorReporter implements ErrorReporter, AutoCloseable {
    @AllArgsConstructor
    @EqualsAndHashCode
    private static class ErrorEntry {
        @StringRes
        private final int summaryStringId;
        private final Throwable error;
    }

    private Subject<ErrorEntry, ErrorEntry> mErrors;
    private Subscription mReporter;

    public DedupingErrorReporter() {
        mErrors = PublishSubject.create();
        mReporter = mErrors.distinctUntilChanged()
                .onBackpressureBuffer()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(e -> reportError(e.summaryStringId, e.error),
                        t -> reportError(R.string.err_misc, t));
    }

    @Override
    public void close() {
        mErrors.onCompleted();
        mReporter.unsubscribe();
    }

    /**
     * @param summaryStringId string resource ID for the error summary
     */
    public void onError(final @StringRes int summaryStringId, final Throwable t) {
        mErrors.onNext(new ErrorEntry(summaryStringId, t));
    }

    protected abstract void reportError(final @StringRes int summaryStringId, final Throwable t);
}
