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
import io.v.android.apps.syncslides.model.Deck;
import io.v.android.apps.syncslides.model.Listener;
import io.v.android.apps.syncslides.model.Participant;

/**
 * Singleton Discovery manager.
 *
 * Scans a mounttable to look for presentations, and permits mounting of
 * a service representing a live presentation.
 */
public class DiscoveryManager implements DB.DBList<Deck>, Moderator.Observer {
    private static final String TAG = "DiscoveryManager";
    // If true, require a live MT to scan.  If false, fake the scan.
    public static final boolean PRODUCTION_MODE = false;
    // Search result indicator.
    public static final int NOT_FOUND = -1;
    // Manages creation, mounting and unmounting of V23 services.
    private final V23Manager mV23Manager;
    // Scans a mount table to understand who's 'giving a presentation', hence
    // the name moderator.  With each scan, determines who's new
    // (freshman), still there (senior) and gone (graduated).
    private final Moderator mModerator;
    // Runs the moderator's scan repeatedly.
    private final PeriodicTasker mTasker = new PeriodicTasker();
    private final List<Participant> mParticipants = new ArrayList<>();
    private Listener mListener;
    private final Handler mHandler;

    private DiscoveryManager(V23Manager manager, Moderator moderator) {
        mV23Manager = manager;
        mModerator = moderator;
        mHandler = new Handler(Looper.getMainLooper());
    }

    public DiscoveryManager start(Context context) {
        if (mListener == null) {
            throw new IllegalStateException("Must have a listener.");
        }
        Log.d(TAG, "Starting");
        if (PRODUCTION_MODE) {
            if (mV23Manager == null) {
                throw new IllegalStateException("Must have V23.");
            }
            mV23Manager.init(context);
        }
        // The observer is the guy who implements onTaskDone, and wants
        // to be notified when a scan is complete.
        mModerator.setObserver(this);
        mTasker.start(mModerator);
        return this;
    }

    public void stop() {
        Log.d(TAG, "Stopping discovery");
        mTasker.stop();
    }

    /**
     * Stops discovery and underlying V23 services.
     */
    public void stopEverything() {
        Log.d(TAG, "Stopping everything.");
        stop();
        if (PRODUCTION_MODE) {
            if (mV23Manager == null) {
                throw new IllegalStateException("Must have V23.");
            }
            mV23Manager.shutdown(V23Manager.Behavior.STRICT);
        }
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
        mListener = null;
    }

    public static class Singleton {
        private static volatile DiscoveryManager instance;

        public static DiscoveryManager get() {
            DiscoveryManager result = instance;
            if (instance == null) {
                synchronized (Singleton.class) {
                    result = instance;
                    if (result == null) {
                        instance = result = makeInstance();
                    }
                }
            }
            return result;
        }

        private static DiscoveryManager makeInstance() {
            Log.d(TAG, "Creating singleton.");
            if (PRODUCTION_MODE) {
                V23Manager manager = V23Manager.Singleton.get();
                return new DiscoveryManager(
                        manager,
                        new Moderator(new ParticipantScannerMt(manager)));
            }
            return new DiscoveryManager(
                    null, new Moderator(new ParticipantScannerFake()));
        }
    }
}



