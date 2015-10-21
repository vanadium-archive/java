// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.syncslides.discovery;

import android.graphics.Bitmap;
import android.util.Log;

import java.io.ByteArrayOutputStream;

import io.v.android.apps.syncslides.db.VDeck;
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

    public VDeck get(VContext ctx, ServerCall call)
            throws io.v.v23.verror.VException {
        Log.d(TAG, "Responding to Get RPC.");
        Log.d(TAG, "  Sending mDeck = " + mDeck);
        VDeck d = new VDeck();
        d.setTitle(mDeck.getTitle());
        if (mDeck.getThumb() == null) {
            Log.d(TAG, "  The response deck has no thumb.");
        } else {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            Bitmap bitmap = mDeck.getThumb();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 60, stream);
            d.setThumbnail(stream.toByteArray());
        }
        return d;
    }
}
