// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;

import io.v.android.v23.services.blessing.BlessingService;
import io.v.v23.security.Blessings;
import lombok.Getter;
import lombok.experimental.Accessors;
import rx.subjects.ReplaySubject;

/**
 * Utility fragment for seeking blessings from the Vanadium Account Manager. This fragment is
 * short-lived, starting the account manager activity in {@link #onCreate(Bundle)} and removing
 * itself in {@link #onActivityResult(int, int, Intent)}.
 */
@Accessors(prefix = "m")
public class BlessingRequestFragment extends Fragment {
    private static final int BLESSING_REQUEST = 0;

    @Getter
    private final ReplaySubject<Blessings> mObservable = ReplaySubject.createWithSize(1);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = BlessingService.newBlessingIntent(getActivity());
        startActivityForResult(intent, BLESSING_REQUEST);
    }

    @Override
    public void onDestroy() {
        mObservable.onCompleted();
        super.onDestroy();
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case BLESSING_REQUEST:
                try {
                    mObservable.onNext(BlessingsUtils.fromActivityResult(resultCode, data));
                    mObservable.onCompleted();
                } catch (final Exception e) {
                    mObservable.onError(e);
                }
                getFragmentManager().beginTransaction()
                        .remove(this)
                        .commit();
                break;
            default:
        }
    }
}
