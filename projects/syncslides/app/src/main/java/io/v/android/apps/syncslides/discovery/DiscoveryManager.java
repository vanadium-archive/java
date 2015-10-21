// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.syncslides.discovery;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import io.v.android.apps.syncslides.db.DB;
import io.v.android.apps.syncslides.misc.Config;
import io.v.android.apps.syncslides.misc.V23Manager;
import io.v.android.apps.syncslides.model.Deck;
import io.v.android.apps.syncslides.model.Listener;
import io.v.android.apps.syncslides.model.Participant;

/**
 * Singleton Discovery manager.
 *
 * Scans a mounttable to look for presentations, and permits mounting of a
 * service representing a live presentation.
 */
public class DiscoveryManager implements DB.DBList<Deck>, Moderator.Observer {
    private static final String TAG = "DiscoveryManager";
    // Search result indicator.
    public static final int NOT_FOUND = -1;
    // Scans a mount table to understand who's 'giving a presentation', hence
    // the name moderator.  With each scan, determines who's new
    // (freshman), still there (senior) and gone (graduated).
    private final Moderator mModerator;
    // Runs the moderator's scan repeatedly.
    private final PeriodicTasker mTasker = new PeriodicTasker();
    private final List<Participant> mParticipants = new ArrayList<>();
    private final Handler mHandler;
    private Listener mListener;

    public static DiscoveryManager make(Context context) {
        // If blessings not in place, use fake data.
        boolean useRealDiscovery =
                Config.MtDiscovery.ENABLE &&
                        V23Manager.Singleton.get().isBlessed();
        if (useRealDiscovery) {
            Log.d(TAG, "Using real discovery.");
            return new DiscoveryManager(
                    new Moderator(new ParticipantScannerMt(context)));
        }
        Log.d(TAG, "Using fake discovery.");
        return new DiscoveryManager(
                new Moderator(new ParticipantScannerFake(context)));
    }

    private DiscoveryManager(Moderator moderator) {
        mModerator = moderator;
        mHandler = new Handler(Looper.getMainLooper());
    }

    public DiscoveryManager start(Context context) {
        if (mListener == null) {
            throw new IllegalStateException("Must have a listener.");
        }
        Log.d(TAG, "Starting");
        // The observer is the guy who implements onTaskDone, and wants
        // to be notified when a scan is complete.
        mModerator.setObserver(this);
        mTasker.start(mModerator);
        Log.d(TAG, "Done Starting");
        return this;
    }

    public void stop() {
        Log.d(TAG, "Stopping discovery");
        mTasker.stop();
    }

    @Override
    public void onTaskDone() {
        for (Participant p : mModerator.getFreshman()) {
            assert mParticipants.indexOf(p) == NOT_FOUND;
            final Participant fp = p;
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mParticipants.add(0, fp);
                    mListener.notifyItemInserted(0);
                }
            });
        }
        for (Participant p : mModerator.getGraduated()) {
            final Participant fp = p;
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    int index = mParticipants.indexOf(fp);
                    assert index != NOT_FOUND;
                    mParticipants.remove(index);
                    mListener.notifyItemRemoved(index);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return mParticipants.size();
    }

    @Override
    public Deck get(int i) {
        return mParticipants.get(i).getDeck();
    }

    @Override
    public void setListener(Listener listener) {
        mListener = listener;
    }

    @Override
    public void discard() {
        Log.d(TAG, "Discarding.");
        stop();
    }
}



