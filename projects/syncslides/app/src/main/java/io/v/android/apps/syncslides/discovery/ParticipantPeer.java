// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.syncslides.discovery;

import android.graphics.Bitmap;
import android.util.Log;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.ByteArrayOutputStream;

import io.v.android.apps.syncslides.db.VDeck;
import io.v.android.apps.syncslides.db.VPerson;
import io.v.android.apps.syncslides.discovery.Presentation;
import io.v.android.apps.syncslides.misc.V23Manager;
import io.v.android.apps.syncslides.model.Deck;
import io.v.android.apps.syncslides.model.DeckFactory;
import io.v.android.apps.syncslides.model.Participant;
import io.v.v23.context.VContext;
import io.v.v23.rpc.ServerCall;
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
    private final String mServiceName;
    // Person presenting.
    private VPerson mUser;
    // When did we last grab data from the endPoint?
    private DateTime mRefreshTime;
    // Deck the user is presenting.  Can only present one at a time.
    private Deck mDeck;
    // The presentation ID.
    private String mPresentationId;
    // The syncgroup name.
    private String mSyncgroupName;
    // Used to make decks after RPCs.
    private final DeckFactory mDeckFactory;

    private ParticipantPeer(
            VPerson user, Deck deck, String serviceName, DeckFactory deckFactory) {
        mUser = user;
        mDeck = deck;
        mServiceName = serviceName;
        mDeckFactory = deckFactory;
    }

    public static ParticipantPeer makeWithServiceName(
            String serviceName, DeckFactory deckFactory) {
        return new ParticipantPeer(
                // The person and deck are obtained from the given service.
                null, null, serviceName, deckFactory);
    }

    public static ParticipantPeer makeWithKnownDeck(VPerson user, Deck deck) {
        return new ParticipantPeer(user, deck, Unknown.SERVER_NAME, null);
    }

    @Override
    public String getServiceName() {
        return (mServiceName != null && !mServiceName.isEmpty()) ?
                mServiceName : Unknown.SERVER_NAME;
    }

    @Override
    public VPerson getUser() {
        return mUser;
    }

    @Override
    public Deck getDeck() {
        return mDeck;
    }

    @Override
    public String getPresentationId() {
        return mPresentationId;
    }

    @Override
    public String getSyncgroupName() {
        return mSyncgroupName;
    }

    @Override
    public String toString() {
        return "[user=\"" + mUser +
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
        boolean deckEqual = (mDeck == null) ? true : mDeck.equals(p.mDeck);
        return deckEqual && getServiceName().equals(p.getServiceName());
    }

    @Override
    public int hashCode() {
        int deckCode = (mDeck == null) ? 0 : mDeck.hashCode();
        return deckCode + getServiceName().hashCode();
    }

    /**
     * Make an RPC on the mServiceName to get title, snapshot, etc.
     */
    @Override
    public boolean refreshData() {
        if (mServiceName.equals(Unknown.SERVER_NAME)) {
            // Don't attempt refresh.
            return true;
        }
        Log.d(TAG, "refreshData");
        // Flush, since the server might have died and restarted, invalidating
        // cached endpoints.
        Log.d(TAG, "Flushing cache for service " + mServiceName);
        V23Manager.Singleton.get().flushServerFromCache(mServiceName);
        ParticipantClient client =
                ParticipantClientFactory.getParticipantClient(mServiceName);
        Log.d(TAG, "Got client = " + client.toString());
        try {
            Log.d(TAG, "Starting RPC to service \"" + mServiceName + "\"...");
            Presentation p = client.get(
                    V23Manager.Singleton.get().getVContext().withTimeout(
                            Duration.standardSeconds(5)));
            mDeck = mDeckFactory.make(p.getDeck(), p.getDeckId());
            mSyncgroupName = p.getSyncgroupName();
            mPresentationId = p.getPresentationId();
            mRefreshTime = DateTime.now();
            Log.d(TAG, "   Discovered:");
            Log.d(TAG, "               mDeck = " + mDeck);
            Log.d(TAG, "      mSyncgroupName = " + mSyncgroupName);
            Log.d(TAG, "     mPresentationId = " + mPresentationId);
            return true;
        } catch (VException e) {
            Log.d(TAG, "RPC failed, leaving current deck in place.");
            e.printStackTrace();
        }
        return false;
    }

    private static class Unknown {
        static final String SERVER_NAME = "unknownServerName";
        static final String USER_NAME = "unknownUserName";
    }

    /**
     * Serves data used in deck discovery.
     */
    public static class Server implements ParticipantServer {
        private static final String TAG = "ParticipantServer";
        private final Presentation mPresentation;

        public Server(
                VDeck d, String deckId, VPerson user,
                String syncGroupName, String presentationId) {
            Presentation p = new Presentation();
            p.setDeck(d);
            p.setDeckId(deckId);
            p.setPerson(user);
            p.setSyncgroupName(syncGroupName);
            p.setPresentationId(presentationId);
            mPresentation = p;
        }

        public Presentation get(VContext ctx, ServerCall call)
                throws VException {
            Log.d(TAG, "Responding to Get RPC.");
            return mPresentation;
        }
    }
}
