// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.syncslides.model;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;

import com.google.common.io.ByteStreams;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import io.v.android.apps.syncslides.R;
import io.v.android.apps.syncslides.db.VDeck;

/**
 * One place to make consistent decks with known defaults.
 */
public class DeckFactory {
    private static final String TAG = "DeckFactory";
    protected final byte[] mDefaultThumb;
    private final Context mContext;

    // Singleton
    private DeckFactory(Context c) {
        mContext = c;
        mDefaultThumb = imageDataFromResource(c, R.drawable.thumb_deck1);
    }

    public Deck make() {
        return make(Unknown.TITLE, Unknown.ID);
    }

    public Deck make(String title, String id) {
        return make(title, mDefaultThumb, id);
    }

    public Deck make(String title, int index, int id) {
        return make(title, imageDataFromResource(mContext, index), String.valueOf(id));
    }

    public Deck make(VDeck vDeck, String id) {
        if (vDeck.getThumbnail() == null) {
            Log.d(TAG, "vDeck missing thumb; vdeck = " + vDeck);
        }
        return make(vDeck.getTitle(), vDeck.getThumbnail(), id);
    }

    public VDeck make(Deck deck) {
        VDeck vd = new VDeck();
        vd.setTitle(deck.getTitle());
        vd.setThumbnail(deck.getThumbData());
        return vd;
    }

    public Deck make(String title, byte[] thumb, String id) {
        title = title == null || title.isEmpty() ? Unknown.TITLE : title;
        thumb = thumb == null ? mDefaultThumb : thumb;
        id = id == null || id.isEmpty() ? Unknown.ID : id;
        return new DeckImpl(title, thumb, id);
    }

    /**
     * Decodes raw image data from the given resource.  Returns empty data on error.
     */
    public static byte[] imageDataFromResource(Context c, int id) {
        try {
            return ByteStreams.toByteArray(c.getResources().openRawResource(id));
        } catch (IOException e) {
            Log.e(TAG, "Error reading image data from resource: " + e.getMessage());
            return new byte[0];
        }
    }

    public static class Singleton {
        private static volatile DeckFactory instance;

        public static DeckFactory get(Context c) {
            DeckFactory result = instance;
            if (instance == null) {
                synchronized (Singleton.class) {
                    result = instance;
                    if (result == null) {
                        instance = result = new DeckFactory(c);
                    }
                }
            }
            return result;
        }

        public static DeckFactory get() {
            if (instance == null) {
                throw new IllegalStateException("Must initialize with context.");
            }
            return instance;
        }
    }

    private static class Unknown {
        static final String TITLE = "unknownTitle";
        static final String ID = "unknownId";
    }
}
