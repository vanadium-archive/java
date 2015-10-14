// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.syncslides.discovery;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import io.v.android.apps.syncslides.R;
import io.v.android.apps.syncslides.model.Deck;
import io.v.android.apps.syncslides.model.DeckImpl;
import io.v.android.apps.syncslides.model.Listener;

/**
 * A fake implementation of Discovery for manual testing purposes.
 */
public class FakeDiscovery implements Discovery {

    private static final String TAG = "FakeDiscovery";
    private static final int[] THUMBS = {
            R.drawable.thumb_deck1,
            R.drawable.thumb_deck2,
            R.drawable.thumb_deck3
    };
    private static final String[] TITLES = {"discovery 1", "discovery 2", "discovery 3"};

    private final Bitmap[] mThumbs;

    public FakeDiscovery(Context context) {
        mThumbs = new Bitmap[THUMBS.length];
        for (int i = 0; i < THUMBS.length; i++) {
            mThumbs[i] = BitmapFactory.decodeResource(context.getResources(), THUMBS[i]);
        }
    }

    private class FakeDList implements DList {
        private final Bitmap[] mThumbs;
        private final String[] mTitles;
        private Listener mListener;

        private FakeDList(Bitmap[] thumbs, String[] titles) {
            mThumbs = thumbs;
            mTitles = titles;
        }

        @Override
        public int getItemCount() {
            return mThumbs.length;
        }

        @Override
        public Deck get(int i) {
            return new DeckImpl(mTitles[i], mThumbs[i]);
        }

        @Override
        public void setListener(Listener listener) {
            assert mListener == null;
            mListener = listener;
        }

        @Override
        public void discard() {
        }
    }

    @Override
    public DList getLivePresentations() {
        return new FakeDList(mThumbs, TITLES);
    }

    @Override
    public void startLivePresentation(
            String deckId, String presentationId, String title, Bitmap thumb) {
    }

    @Override
    public void stopLivePresentation(String deckId, String presentationId) {
    }
}
