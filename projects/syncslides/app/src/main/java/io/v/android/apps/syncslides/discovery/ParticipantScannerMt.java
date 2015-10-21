// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.syncslides.discovery;

import android.content.Context;
import android.util.Log;

import java.util.HashSet;
import java.util.Set;

import io.v.android.apps.syncslides.misc.Config;
import io.v.android.apps.syncslides.misc.V23Manager;
import io.v.android.apps.syncslides.model.DeckFactory;
import io.v.android.apps.syncslides.model.Participant;

/**
 * Scan a V23 MT for syncslides participants.
 */
public class ParticipantScannerMt implements ParticipantScanner {
    private static final String TAG = "ParticipantScannerMt";

    protected final DeckFactory mDeckFactory;

    public ParticipantScannerMt(Context context) {
        this.mDeckFactory = DeckFactory.Singleton.get(context);
    }

    @Override
    public Set<Participant> scan() {
        Set<Participant> result = new HashSet<>();
        for (String n : V23Manager.Singleton.get().scan(
                Config.MtDiscovery.makeScanString())) {
            Log.d(TAG, "Found: " + n);
            result.add(ParticipantPeer.makeWithServiceName(n, mDeckFactory));
        }
        return result;
    }
}
