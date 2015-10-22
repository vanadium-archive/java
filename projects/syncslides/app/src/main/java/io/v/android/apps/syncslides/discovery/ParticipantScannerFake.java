// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.syncslides.discovery;

import android.content.Context;

import java.util.HashSet;
import java.util.Set;

import io.v.android.apps.syncslides.db.VPerson;
import io.v.android.apps.syncslides.model.DeckFactory;
import io.v.android.apps.syncslides.model.Participant;

public class ParticipantScannerFake implements ParticipantScanner {
    private static final String TAG = "ParticipantScannerFake";

    protected final DeckFactory mDeckFactory;

    private int mCounter = 0;

    public ParticipantScannerFake(Context context) {
        this.mDeckFactory = DeckFactory.Singleton.get(context);
    }

    public Set<Participant> scan() {
        mCounter = (mCounter + 1) % 10;
        HashSet<Participant> participants = new HashSet<>();
        if (mCounter >= 2 && mCounter <= 8) {
            participants.add(
                    ParticipantPeer.makeWithKnownDeck(
                            new VPerson("Liz", "Lemon"),
                            mDeckFactory.make(
                                    "Kale - Just eat it.",
                                    "deckByAlice")));
        }
        // Bob has less to say than Alice.
        if (mCounter >= 4 && mCounter <= 6) {
            participants.add(
                    ParticipantPeer.makeWithKnownDeck(
                            new VPerson("Jack", "Donaghy"),
                            mDeckFactory.make(
                                    "Java - Object deluge.",
                                    "deckByBob")));
        }
        return participants;
    }
}
