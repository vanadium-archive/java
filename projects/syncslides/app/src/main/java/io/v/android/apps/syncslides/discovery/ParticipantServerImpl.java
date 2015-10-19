// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.syncslides.discovery;

import android.util.Log;

import io.v.android.apps.syncslides.model.Deck;
import io.v.v23.context.VContext;
import io.v.v23.rpc.ServerCall;

/**
 * Serves data used in deck discovery.
 */
public class ParticipantServerImpl implements ParticipantServer {
    private static final String TAG = "PresentationActivity";
    private final Deck mDeck;

    public ParticipantServerImpl(Deck d) {
        mDeck = d;
    }

    public Description get(VContext ctx, ServerCall call)
            throws io.v.v23.verror.VException {
        Log.d(TAG, "Responding to Get RPC.");
        Description d = new Description();
        d.setTitle(mDeck.getTitle());
        d.setUserName(mDeck.getId());
        return d;
    }
}
