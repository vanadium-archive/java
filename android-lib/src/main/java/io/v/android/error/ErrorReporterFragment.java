// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.error;

import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.support.annotation.StringRes;

public class ErrorReporterFragment extends Fragment implements ErrorReporter {
    public static final String FRAGMENT_TAG = ErrorReporterFragment.class.getName();

    public static ErrorReporterFragment find(final FragmentManager mgr) {
        return (ErrorReporterFragment) mgr.findFragmentByTag(FRAGMENT_TAG);
    }

    private DedupingErrorReporter mBaseReporter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBaseReporter = createErrorReporter();
    }

    protected DedupingErrorReporter createErrorReporter() {
        return new ToastingErrorReporter(getActivity());
    }

    @Override
    public void onDestroy() {
        mBaseReporter.close();
        super.onDestroy();
    }

    @Override
    public void onError(@StringRes int summaryStringId, Throwable t) {
        mBaseReporter.onError(summaryStringId, t);
    }
}
