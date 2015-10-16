// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.syncslides.discovery;

import android.util.Log;

import java.util.HashSet;
import java.util.Set;

import io.v.android.apps.syncslides.model.Participant;

/**
 * Moderator is a task that figures out who's presenting.
 *
 * On each call to run(), it scans a MT for information about participant
 * services, then contacts those participants for information on what they are
 * presenting. When a scan is complete, it calls mObserver.onTaskDone().
 *
 * Not safe for more than one thread to enter run() at a time.
 */
class Moderator implements Runnable {

    private static final String TAG = "Moderator";

    // Participants that appeared for the first time in the most recent scan.
    private final Set<Participant> mFreshman = new HashSet<>();

    // Participants seen currently present for more than one scan.
    private final Set<Participant> mSenior = new HashSet<>();

    // Participants that were not seen in the most recent scan, but we there in
    // the scan just before that.
    private final Set<Participant> mGraduated = new HashSet<>();
    // Does the actual scan.
    private final ParticipantScanner mScanner;
    // Notify this guy when task done; make it a list if more needed.
    private Observer mObserver;
    // Counts runs for debugging.
    private int mCounter = 0;

    public Moderator(ParticipantScanner scanner) {
        mScanner = scanner;
    }

    public void setObserver(Observer observer) {
        this.mObserver = observer;
    }

    public Set<Participant> getGraduated() {
        return mGraduated;
    }

    public Set<Participant> getFreshman() {
        return mFreshman;
    }

    @Override
    public void run() {
        if (mObserver == null) {
            throw new IllegalStateException("Must have an observer.");
        }
        try {
            mCounter++;
            process(mScanner.scan());
            mObserver.onTaskDone();
        } catch (Throwable t) {
            Log.e(TAG, "Scan Failed.", t);
        }
    }

    private void process(Set<Participant> latest) {
        Set<Participant> current = new HashSet<>();
        current.addAll(mSenior);
        current.addAll(mFreshman);

        mSenior.clear();
        mFreshman.clear();

        for (Participant p : latest) {
            if (current.contains(p)) {
                mSenior.add(p);
            } else {
                mFreshman.add(p);
            }
        }

        mGraduated.clear();
        for (Participant p : current) {
            if (!latest.contains(p)) {
                mGraduated.add(p);
            }
        }

        for (Participant p : mFreshman) {
            p.refreshData();
        }
    }

    public interface Observer {
        void onTaskDone();
    }
}
