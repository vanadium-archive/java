// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.syncslides.model;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;

/**
 * An implementation of {@link Deck} interface.
 */
public class DeckImpl implements Deck {
    private final String mTitle;
    private final byte[] mThumb;
    private final String mDeckId;

    public DeckImpl(String title, byte[] thumb, String deckId) {
        mTitle = title;
        mThumb = thumb;
        mDeckId = deckId;
    }

    public String toString() {
        return "[title=\"" + (mTitle == null ? "unknown" : mTitle) +
                "\", id=" + (mDeckId == null ? "unknown" : mDeckId) +
                ", thumb=" + (mThumb == null ? "no" : "yes") + "]";
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof DeckImpl)) {
            return false;
        }
        DeckImpl p = (DeckImpl) obj;
        return mDeckId.equals(p.mDeckId);
    }
    @Override
    public int hashCode() {
        return mDeckId.hashCode();
    }
    @Override
    public Bitmap getThumb() {
        return BitmapFactory.decodeByteArray(mThumb, 0, mThumb.length);
    }
    @Override
    public byte[] getThumbData() {
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

}
