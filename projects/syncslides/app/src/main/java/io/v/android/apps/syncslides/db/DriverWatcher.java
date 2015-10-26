// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.syncslides.db;

import android.util.Log;

import io.v.impl.google.naming.NamingUtil;
import io.v.v23.syncbase.nosql.BatchDatabase;
import io.v.v23.syncbase.nosql.ChangeType;
import io.v.v23.syncbase.nosql.Stream;
import io.v.v23.syncbase.nosql.Table;
import io.v.v23.syncbase.nosql.WatchChange;
import io.v.v23.verror.VException;
import io.v.v23.vom.VomUtil;

/**
 * Watches for a new driver (person controlling the slides) in a given presentation.
 */
class DriverWatcher {
    private static final String TAG = "DriverWatcher";
    private final WatcherState mState;
    private final DB.DriverListener mListener;
    private boolean mIsDiscarded;

    /**
     * Creates a new watcher for the presentation in state.
     *
     * @param state    objects necessary to do the watching
     * @param listener notified whenever the driver changes
     */
    public DriverWatcher(WatcherState state, DB.DriverListener listener) {
        mState = state;
        mListener = listener;
        mState.thread = new Thread(new Runnable() {
            @Override
            public void run() {
                watch();
            }
        });
        mState.thread.start();
        mIsDiscarded = false;
    }

    /**
     * Stops watching the presentation for a new driver.
     */
    public void discard() {
        mState.vContext.cancel();  // this will cause the watcher thread to exit
        mState.handler.removeCallbacksAndMessages(null);
        // We've canceled all the pending callbacks, but the handler might be just about
        // to execute put()/get() and those messages wouldn't get canceled.  So we mark
        // the list as discarded and count on put()/get() checking for it.
        mIsDiscarded = true;
    }

    private void watch() {
        try {
            String row = NamingUtil.join(mState.deckId, mState.presentationId);
            Log.i(TAG, "Watching driver: " + row);
            BatchDatabase batch = mState.db.beginBatch(mState.vContext, null);
            Table presentations = batch.getTable(SyncbaseDB.PRESENTATIONS_TABLE);
            if (presentations.getRow(row).exists(mState.vContext)) {
                VPresentation presentation = (VPresentation) presentations.get(
                        mState.vContext, row, VPresentation.class);
                postInUiThread(presentation.getDriver().getElem());
            }

            Stream<WatchChange> watch = mState.db.watch(
                    mState.vContext, SyncbaseDB.PRESENTATIONS_TABLE, row,
                    batch.getResumeMarker(mState.vContext));
            for (WatchChange change : watch) {
                Log.i(TAG, "Found change " + change.getChangeType());
                if (!change.getRowName().equals(row)) {
                    continue;
                }
                if (change.getChangeType().equals(ChangeType.PUT_CHANGE)) {
                    final VPresentation presentation = (VPresentation) VomUtil.decode(
                            change.getVomValue(), VPresentation.class);
                    postInUiThread(presentation.getDriver().getElem());
                } else { // ChangeType.DELETE_CHANGE
                    postInUiThread(null);
                }
            }
        } catch (VException e) {
            e.printStackTrace();
        }
    }

    private void postInUiThread(final VPerson driver) {
        mState.handler.post(new Runnable() {
            @Override
            public void run() {
                if (!mIsDiscarded) {
                    mListener.onChange(driver);
                }
            }
        });

    }
}

