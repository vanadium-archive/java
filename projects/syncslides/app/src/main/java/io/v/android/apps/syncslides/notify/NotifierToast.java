// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.syncslides.notify;

import android.app.Activity;
import android.widget.Toast;

import io.v.android.apps.syncslides.R;
import io.v.android.apps.syncslides.model.Notifier;
import io.v.android.apps.syncslides.model.Participant;

/**
 * Posts a toast about a presentation that's newly available.
 */
public class NotifierToast implements Notifier {
    private static final String TAG = "NotifierToast";

    private Activity mActivity;

    public NotifierToast(Activity activity, Class<?> klass) {
        mActivity = activity;
    }

    public void show(Participant p) {
        Toast.makeText(
                mActivity.getApplicationContext(),
                mActivity.getResources().getString(R.string.presentation_live) +
                        "   " + p.getDeck().getTitle(),
                Toast.LENGTH_LONG).show();
    }

    /**
     * Does nothing, since toasts are ephemeral.
     */
    public void dismiss() {
    }
}
