// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.syncslides.notify;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;

import io.v.android.apps.syncslides.R;
import io.v.android.apps.syncslides.model.Notifier;
import io.v.android.apps.syncslides.model.Participant;

/**
 * Posts an Android notification about a presentation that's newly available.
 * When clicked, user taken to an appropriate activity.
 *
 * TODO(jregan): Fix this so it does the right thing. Right now, if the app is
 * up, clicking on the notifier destroys all state, and there's a delay to
 * recreate it all.
 */
public class NotifierNative implements Notifier {
    private static final String TAG = "NotifierNative";

    /**
     * TODO(jregan): What should this be?
     */
    private static final int UNKNOWN_PI_REQ_CODE = 0;

    private Activity mActivity;
    private Class<?> mClass;
    private NotificationManager mManager;

    public NotifierNative(Activity activity, Class<?> klass) {
        mActivity = activity;
        mClass = klass;
    }

    public void show(Participant p) {
        PendingIntent contentIntent =
                PendingIntent.getActivity(
                        mActivity, UNKNOWN_PI_REQ_CODE,
                        new Intent(mActivity, mClass),
                        PendingIntent.FLAG_CANCEL_CURRENT);

        mManager = (NotificationManager) mActivity.getSystemService(
                mActivity.NOTIFICATION_SERVICE);
        Notification notification = new Notification.Builder(mActivity)
                .setContentIntent(contentIntent)
                .setFullScreenIntent(contentIntent, false)
                .setAutoCancel(true)
                .setWhen(System.currentTimeMillis())
                .setContentTitle(p.getUser().getName())
                .setContentText(p.getDeck().getTitle())
                // TODO(jregan): Need a better icon.
                .setSmallIcon(R.drawable.orange_circle)
                .build();
        // This particular resource id acts merely as a unique number
        // usable for cancellation (see #unshowNotification).
        mManager.notify(R.string.presentation_live, notification);
    }

    /**
     * Remove notification, if present.
     */
    public void dismiss() {
        if (mManager != null) {
            mManager.cancel(R.string.presentation_live);
            mManager = null;
        }
    }
}
