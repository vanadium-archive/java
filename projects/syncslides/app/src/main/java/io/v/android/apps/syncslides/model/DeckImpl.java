// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.syncslides.model;

import android.graphics.Bitmap;

/**
 * Application impl of Deck.
 */
public class DeckImpl implements Deck {
    // For demos, debugging.
    public static final Deck DUMMY = new DeckImpl(
            Unknown.TITLE, Unknown.THUMB, Unknown.ID);

    private final String mTitle;
    private final Bitmap mThumb;
    private final String mDeckId;

    public DeckImpl(String title, Bitmap thumb, String deckId) {
        mTitle = title;
        mThumb = thumb;
        mDeckId = deckId;
    }

    public DeckImpl(String title, Bitmap thumb) {
        this(title, thumb, Unknown.ID);
    }

    public DeckImpl(String title) {
        this(title, Unknown.THUMB);
    }

    @Override
    public Bitmap getThumb() {
        return mThumb;
    }

    @Override
    public String getTitle() {
        return mTitle;
    }

    @Override
    public String getId() {
        return mDeckId;
    }

    private static class Unknown {
        static final String TITLE = "unknownTitle";
        static final String ID = "unknownId";
        static final Bitmap THUMB = null;
    }
}
