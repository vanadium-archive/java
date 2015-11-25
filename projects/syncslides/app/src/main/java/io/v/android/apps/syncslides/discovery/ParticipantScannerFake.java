// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.syncslides.discovery;

import android.content.Context;

import java.util.HashSet;
import java.util.Set;

import com.google.common.base.Joiner;

import io.v.android.apps.syncslides.db.VPerson;
import io.v.android.apps.syncslides.model.DeckFactory;
import io.v.android.apps.syncslides.model.Participant;
import io.v.v23.security.Constants;

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
                            new VPerson(
                                    Joiner.on(Constants.CHAIN_SEPARATOR).join(
                                            "dev.v.io",
                                            "u",
                                            "liz.lemon@gmail.com",
                                            "android",
                                            "io.v.android.apps.syncslides"),
                                    "Liz Lemon"),
                            mDeckFactory.make(
                                    "Kale - Just eat it.",
                                    "deckByAlice")));
        }
        // Jack has less to say than Liz.
        if (mCounter >= 4 && mCounter <= 6) {
            participants.add(
                    ParticipantPeer.makeWithKnownDeck(
                            new VPerson(
                                    Joiner.on(Constants.CHAIN_SEPARATOR).join(
                                            "dev.v.io",
                                            "u",
                                            "jack.donaghy@gmail.com",
                                            "android",
                                            "io.v.android.apps.syncslides"),
                                    "Jack Donaghy"),
                            mDeckFactory.make(
                                    "Java - Object deluge.",
                                    "deckByBob")));
        }
        return participants;
    }
}
