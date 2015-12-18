// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit.blessings;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;

import io.v.android.v23.services.blessing.BlessingService;
import io.v.baku.toolkit.ErrorReporters;
import io.v.v23.security.Blessings;
import lombok.experimental.Accessors;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;
import rx.Observable;
import rx.subjects.ReplaySubject;

@Accessors(prefix = "m")
@Slf4j
public class AccountManagerBlessingsFragment extends Fragment
        implements RefreshableBlessingsProvider {
    public static final String TAG = AccountManagerBlessingsFragment.class.getName();

    private static final int BLESSING_REQUEST = 0;
    private static final String SEEKING =
            AccountManagerBlessingsFragment.class.getName() + ".seeking";

    public static AccountManagerBlessingsFragment find(final FragmentManager mgr) {
        return (AccountManagerBlessingsFragment)mgr.findFragmentByTag(TAG);
    }

    @Delegate(types = RefreshableBlessingsProvider.class, excludes = BlessingsProvider.class)
    private ActivityBlessingsSeeker mSeeker;
    private ReplaySubject<ActivityBlessingsSeeker> mSeekers = ReplaySubject.createWithSize(1);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final boolean resumingSeek = savedInstanceState != null &&
                savedInstanceState.getBoolean(SEEKING);
        mSeeker = new ActivityBlessingsSeeker(
                getActivity(), ErrorReporters.forFragment(this), resumingSeek) {
            @Override
            protected void seekBlessings() {
                final Intent intent = BlessingService.newBlessingIntent(getActivity());
                startActivityForResult(intent, BLESSING_REQUEST);
            }
        };
        mSeekers.onNext(mSeeker);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(SEEKING, mSeeker.isAwaitingBlessings());
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case BLESSING_REQUEST:
                try {
                    mSeeker.setBlessings(BlessingsUtils.fromActivityResult(resultCode, data));
                } catch (final Exception e) {
                    mSeeker.handleBlessingsError(e);
                }
                break;
            default:
        }
    }

    @Override
    public Observable<Blessings> getRxBlessings() {
        return mSeekers.switchMap(ActivityBlessingsSeeker::getRxBlessings);
    }

    @Override
    public Observable<Blessings> getPassiveRxBlessings() {
        return mSeekers.switchMap(ActivityBlessingsSeeker::getPassiveRxBlessings);
    }
}
