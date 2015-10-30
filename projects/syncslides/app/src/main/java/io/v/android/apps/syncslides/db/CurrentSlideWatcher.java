// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.syncslides.db;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.common.collect.Lists;

import java.util.List;

import io.v.impl.google.naming.NamingUtil;
import io.v.v23.VIterable;
import io.v.v23.context.CancelableVContext;
import io.v.v23.context.VContext;
import io.v.v23.syncbase.nosql.BatchDatabase;
import io.v.v23.syncbase.nosql.ChangeType;
import io.v.v23.syncbase.nosql.Database;
import io.v.v23.syncbase.nosql.Table;
import io.v.v23.syncbase.nosql.WatchChange;
import io.v.v23.verror.VException;
import io.v.v23.vom.VomUtil;

/**
 * Watches for changes to the CurrentSlide row.
 */
class CurrentSlideWatcher {

    private static final String TAG = "CurrentSlideWatcher";
    private final CancelableVContext mVContext;
    private final Database mDB;
    private final String mDeckId;
    private final String mPresentationId;
    private final List<DB.CurrentSlideListener> mListeners;
    private final Handler mHandler;
    private VCurrentSlide mCurrentSlide;
    private Thread mThread;

    public CurrentSlideWatcher(VContext vContext, Database db, String deckId,
                               String presentationId) {
        mVContext = vContext.withCancel();
        mDB = db;
        mDeckId = deckId;
        mPresentationId = presentationId;
        mListeners = Lists.newArrayList();
        mHandler = new Handler(Looper.getMainLooper());
        mCurrentSlide = new VCurrentSlide(0);
        mThread = new Thread(new Runnable() {
            @Override
            public void run() {
                watchCurrentSlide();
            }
        });
    }

    /**
     * Adds a listener for this deckId/presentationId.
     *
     * @param listener notified when the current slide changes
     */
    public void addListener(final DB.CurrentSlideListener listener) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                listener.onChange(mCurrentSlide.getNum());
            }
        });
        mListeners.add(listener);
        if (mListeners.size() == 1) {
            // The first listener was just added.
            Log.i(TAG, "Starting thread");
            mThread.start();
        }
    }

    /**
     * Removes a listener previously added with addListener.  If this was the last
     * listener, no more listeners can be added and the CurrentSlideWatcher
     * should be discarded.
     */
    public void removeListener(DB.CurrentSlideListener listener) {
        mListeners.remove(listener);
        if (mListeners.isEmpty()) {
            mVContext.cancel();
            mThread = null;
            mHandler.removeCallbacksAndMessages(null);
        }
    }

    /**
     * Returns true if the number of listeners is greater than zero.
     */
    public boolean hasListeners() {
        return !mListeners.isEmpty();
    }

    private void notifyListeners(VCurrentSlide slide) {
        Log.i(TAG, "notifyListeners " + slide);
        mCurrentSlide = slide;
        for (DB.CurrentSlideListener listener : mListeners) {
            Log.i(TAG, "notifying listener " + listener);
            listener.onChange(mCurrentSlide.getNum());
        }
    }

    private void watchCurrentSlide() {
        try {
            Log.i(TAG, "watchCurrentSlide");
            String rowKey = NamingUtil.join(mDeckId, mPresentationId, SyncbaseDB.CURRENT_SLIDE);
            BatchDatabase batch = mDB.beginBatch(mVContext, null);
            Table presentations = batch.getTable(SyncbaseDB.PRESENTATIONS_TABLE);
            if (presentations.getRow(rowKey).exists(mVContext)) {
                final VCurrentSlide slide = (VCurrentSlide) presentations.get(
                        mVContext, rowKey, VCurrentSlide.class);
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        notifyListeners(slide);
                    }
                });
            }
            VIterable<WatchChange> changes =
                    mDB.watch(mVContext, SyncbaseDB.PRESENTATIONS_TABLE, rowKey,
                    batch.getResumeMarker(mVContext));
            for (WatchChange change : changes) {
                if (!change.getTableName().equals(SyncbaseDB.PRESENTATIONS_TABLE)) {
                    Log.e(TAG, "Wrong change table name: " + change.getTableName() + ", wanted: " +
                            SyncbaseDB.PRESENTATIONS_TABLE);
                    continue;
                }
                final String key = change.getRowName();
                Log.i(TAG, "Found change " + key);
                if (!key.equals(rowKey)) {
                    Log.d(TAG, "Ignoring change: " + key);
                    continue;
                }
                if (change.getChangeType().equals(ChangeType.PUT_CHANGE)) {
                    final VCurrentSlide slide2 = (VCurrentSlide) VomUtil.decode(
                            change.getVomValue(), VCurrentSlide.class);
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            notifyListeners(slide2);
                        }
                    });
                }
            }
        } catch (VException e) {
            Log.e(TAG, "Watching failed " + e);
        }
    }
}
