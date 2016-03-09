// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit;

import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.support.annotation.StringRes;
import android.widget.Toast;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.subjects.PublishSubject;
import rx.subjects.Subject;

@Slf4j
public class ErrorReporterFragment extends Fragment implements ErrorReporter {
    public static final String TAG = ErrorReporterFragment.class.getName();

    public static ErrorReporterFragment find(final FragmentManager mgr) {
        return (ErrorReporterFragment) mgr.findFragmentByTag(TAG);
    }

    @AllArgsConstructor
    @EqualsAndHashCode
    private static class ErrorEntry {
        @StringRes
        private final int summaryStringId;
        private final Throwable error;
    }

    private Subject<ErrorEntry, ErrorEntry> mErrors;
    private Subscription mReporter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mErrors = PublishSubject.create();
        mReporter = mErrors.distinctUntilChanged()
                .onBackpressureBuffer()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(e -> reportError(e.summaryStringId, e.error),
                        t -> reportError(R.string.err_misc, t));
    }

    @Override
    public void onDestroy() {
        mErrors.onCompleted();
        mReporter.unsubscribe();
        super.onDestroy();
    }

    /**
     * @param summaryStringId string resource ID for the error summary
     */
    public void onError(final @StringRes int summaryStringId, final Throwable t) {
        mErrors.onNext(new ErrorEntry(summaryStringId, t));
    }

    protected void reportError(final @StringRes int summaryStringId, final Throwable t) {
        log.error(getString(summaryStringId), t);
        Toast.makeText(getActivity(), summaryStringId, Toast.LENGTH_LONG).show();
    }
}
