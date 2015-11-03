// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.syncslides.model;

import android.graphics.Bitmap;
import android.os.Bundle;

/**
 * A deck, aka a presentation.
 */
public interface Deck {
    /**
     * Returns a Bitmap suitable as a thumbnail of the deck (e.g. the title
     * slide).
     */
    Bitmap getThumb();

    /**
     * Returns raw thumbnail data.
     */
    byte[] getThumbData();

    /**
     * Returns the title of the deck.
     */
    String getTitle();

    /**
     * Returns the deck id.
     */
    String getId();

    /**
     * Keys for Bundle/Intent fields.
     */
    class B {
        public static final String DECK_ID = "deck_id";
    }
}
