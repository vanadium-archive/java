// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.syncslides.discovery;

import android.util.Log;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import io.v.android.apps.syncslides.misc.V23Manager;
import io.v.android.apps.syncslides.model.Deck;
import io.v.android.apps.syncslides.model.DeckImpl;
import io.v.android.apps.syncslides.model.Participant;
import io.v.v23.verror.VException;

/**
 * Someone taking part in a presentation.
 *
 * A deck presenter (i.e. a human speaker) is represented by one of these, to
 * let others know information about the deck being presented (title, author,
 * etc.).
 *
 * When someone runs syncslides, they'll hold N of these, one for each live talk
 * out there that they can join.  Those instances won't run as servers; they'll
 * just be parcelable data blobs.
 *
 * When that someone wants to give a presentation, they'll create one of these
 * and run it as a server, using the (public) userName as part of the mount
 * name.
 */
public class ParticipantPeer implements Participant {
    private static final String TAG = "ParticipantPeer";
    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormat.forPattern("hh_mm_ss_SSSS");
    // V23 name of the V23 service representing the participant.
    private String mServiceName;
    // Visible name of human presenter.
    // TODO(jregan): Switch to VPerson or the model equivalent.
    private String mUserName;
    // When did we last grab data from the endPoint?
    private DateTime mRefreshTime;
    // Deck the user is presenting.  Can only present one at a time.
    private Deck mDeck;
    private ParticipantClient mClient = null;

    public ParticipantPeer(String userName, Deck deck, String serviceName) {
        mUserName = userName;
        mDeck = deck;
        mServiceName = serviceName;
    }

    public ParticipantPeer(String userName, Deck deck) {
        this(userName, deck, Unknown.SERVER_NAME);
    }

    public ParticipantPeer(String serviceName) {
        this(Unknown.USER_NAME, DeckImpl.DUMMY, serviceName);
    }

    @Override
    public String getServiceName() {
        return mServiceName;
    }

    @Override
    public String getUserName() {
        return mUserName;
    }

    @Override
    public Deck getDeck() {
        return mDeck;
    }

    @Override
    public String toString() {
        return "[userName=\"" + mUserName +
                "\", deck=" + mDeck +
                ", time=" + getStringRefreshtime() + "]";
    }

    private String getStringRefreshtime() {
        return mRefreshTime == null ?
                "never" : mRefreshTime.toString(TIME_FMT);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ParticipantPeer)) {
            return false;
        }
        ParticipantPeer p = (ParticipantPeer) obj;
        return mServiceName.equals(p.mServiceName) && mDeck.equals(p.mDeck);
    }

    @Override
    public int hashCode() {
        return mServiceName.hashCode() + mDeck.hashCode();
    }

    /**
     * Make an RPC on the mServiceName to get title, snapshot, etc.
     */
    @Override
    public void refreshData() {
        Log.d(TAG, "Initiating refresh");

        if (mClient == null) {
            Log.d(TAG, "Grabbing client.");
            mClient = ParticipantClientFactory.getParticipantClient(
                    mServiceName);
            Log.d(TAG, "Got client.");
        }
        try {
            Log.d(TAG, "Calling get");
            Description description = mClient.get(
                    V23Manager.Singleton.get().getVContext());
            mDeck = new DeckImpl(description.getTitle());
            mRefreshTime = DateTime.now();
            Log.d(TAG, "Completed refresh.");
        } catch (VException e) {
            e.printStackTrace();
        }
    }

    private static class Unknown {
        static final String SERVER_NAME = "unknownServerName";
        static final String USER_NAME = "unknownUserName";
    }
}
