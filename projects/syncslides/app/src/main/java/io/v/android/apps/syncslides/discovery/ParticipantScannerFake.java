// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.syncslides.discovery;

import java.util.HashSet;
import java.util.Set;

import io.v.android.apps.syncslides.model.DeckImpl;
import io.v.android.apps.syncslides.model.Participant;

public class ParticipantScannerFake implements ParticipantScanner {
    private static final String TAG = "ParticipantScannerFake";

    private int mCounter = 0;

    public Set<Participant> scan() {
        mCounter = (mCounter + 1) % 10;
        HashSet<Participant> participants = new HashSet<>();
        if (mCounter >= 2 && mCounter <= 8) {
            participants.add(
                    new ParticipantPeer(
                            "Alice", new DeckImpl(
                            "Kale - Just eat it.", null, "deckByAlice")));
        }
        // Bob has less to say than Alice.
        if (mCounter >= 4 && mCounter <= 6) {
            participants.add(
                    new ParticipantPeer(
                            "Bob", new DeckImpl(
                            "Java - Object deluge.", null, "deckByBob")));
        }
        return participants;
    }
}
