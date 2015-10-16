// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.syncslides.discovery;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import io.v.android.apps.syncslides.Role;
import io.v.android.apps.syncslides.model.Deck;
import io.v.android.apps.syncslides.model.DeckImpl;
import io.v.android.apps.syncslides.model.Participant;
import io.v.v23.context.VContext;
import io.v.v23.rpc.ServerCall;

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
public class ParticipantPeer extends Service implements Participant {
    private static final String TAG = "ParticipantPeer";
    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormat.forPattern("hh_mm_ss_SSSS");
    // V23 EndPoint of the V23 service representing the participant.
    private String mEndpointStr;
    // When did we last grab data from the endPoint?  Meaningful only in
    // 'audience' mode, where the contents of mUserName etc. came from a remote
    // server rather than from being fed into the ctor.
    private DateTime mRefreshTime;
    // Name of the user participating, intended to be visible to others. This
    // can be a colloquial name as opposed to a 'real' name or email address
    // extracted from a device or blessing.
    private String mUserName;
    // Deck the user is presenting.  Can only present one at a time.
    private Deck mDeck;
    // The role of the participant.
    private Role mRole;

    public ParticipantPeer(String userName, Deck deck, String endPoint) {
        mUserName = userName;
        mDeck = deck;
        mEndpointStr = endPoint;
    }

    public ParticipantPeer(String endPoint) {
        this(Unknown.USER_NAME, DeckImpl.DUMMY, endPoint);
    }

    public ParticipantPeer(String userName, Deck deck) {
        this(userName, deck, Unknown.END_POINT);
    }

    public ParticipantPeer() {
        this(Unknown.END_POINT);
    }

    public static Participant fromBundle(Bundle b) {
        return new ParticipantPeer(
                b.getString(B.PARTICIPANT_NAME),
                DeckImpl.fromBundle(b),
                b.getString(B.PARTICIPANT_END_POINT));
    }

    @Override
    public String getEndPoint() {
        return mEndpointStr;
    }

    @Override
    public String getUserName() {
        return mUserName;
    }

    /**
     * TODO(jregan): Assure legal mount name (remove blanks and such).
     */
    public String getMountName() {
        return ParticipantScannerMt.ROOT_NAME + "/" +
                getUserName().toLowerCase().trim();
    }

    @Override
    public Deck getDeck() {
        return mDeck;
    }

    @Override
    public Bundle toBundle() {
        Bundle b = new Bundle();
        b.putString(B.PARTICIPANT_NAME, mUserName);
        b.putString(B.PARTICIPANT_END_POINT, mEndpointStr);
        mDeck.toBundle(b);
        return b;
    }

    private void unpackBundle(Bundle b) {
        mDeck = DeckImpl.fromBundle(b);
        mRole = Role.valueOf(b.getString(B.PARTICIPANT_ROLE));
        mEndpointStr = b.getString(B.PARTICIPANT_END_POINT);
        mUserName = b.getString(B.PARTICIPANT_NAME);
    }

    @Override
    public String toString() {
        return mUserName + ":" + mDeck.getTitle() +
                (mRefreshTime == null ?
                        "" : ":" + mRefreshTime.toString(TIME_FMT));
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Participant)) {
            return false;
        }
        return mEndpointStr.equals(((ParticipantPeer) obj).mEndpointStr);
    }

    @Override
    public int hashCode() {
        return mEndpointStr.hashCode() + mDeck.getTitle().hashCode();
    }

    /**
     * Make an RPC on the mEndpointStr to get title, snapshot, etc.
     */
    @Override
    public void refreshData() {
        Log.d(TAG, "Refreshing data for participant " + mUserName);
        // TODO(jregan): make the rpc
        mRefreshTime = DateTime.now();
    }

    /**
     * Binding not necessary - this service just answers requests from the
     * outside, and doesn't communicate with the parent app.
     */
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.d(TAG, "onStartCommand");
        // TODO(jregan): Unpack blessings from the intent and pass them into
        // V.getPrincipal.
        unpackBundle(intent.getExtras());
        V23Manager mgr = V23Manager.Singleton.get();
        mgr.init(getApplicationContext());
        ServerImpl server = new ServerImpl(this);
        mEndpointStr = mgr.mount(getMountName(), server);
        return START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        V23Manager.Singleton.get().unMount();
        Log.d(TAG, "###### onDestroy");
    }

    private static class Unknown {
        static final String END_POINT = "unknownEndPoint";
        static final String USER_NAME = "unknownUserName";
    }

    /**
     * Implementation of VDL Participant service.
     */
    private class ServerImpl implements ParticipantServer {
        private final Participant mParticipant;

        public ServerImpl(Participant p) {
            mParticipant = p;
        }

        public Description get(VContext ctx, ServerCall call)
                throws io.v.v23.verror.VException {
            Description d = new Description();
            d.setTitle(mParticipant.getDeck().getTitle());
            d.setUserName(mParticipant.getUserName());
            return d;
        }
    }
}
