// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.syncslides;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toolbar;

/**
 * Provides a list of decks to be shown in the RecyclerView of the
 * DeckChooserFragment.
 */
public class DeckListAdapter extends RecyclerView.Adapter<DeckListAdapter.ViewHolder> {
    // TODO(kash): Replace this static data with syncbase.
    private static final int[] THUMBS = {
            R.drawable.thumb_deck1,
            R.drawable.thumb_deck2,
            R.drawable.thumb_deck3
    };
    private static final String[] TITLES = {"deck 1", "deck 2", "deck 3"};


    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int i) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.deck_card, parent, false);
        // TODO(kash): Add a menu that allows the user to delete a deck.
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int i) {
        holder.mToolbar.setTitle(TITLES[i]);
        // TODO(kash): We need to say when the user last viewed the deck or show
        // that the deck is active.  Either use the subtitle for this or create
        // a custom view for both the title and subtitle.
        holder.mThumb.setImageResource(THUMBS[i]);
    }

    @Override
    public int getItemCount() {
        return TITLES.length;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final ImageView mThumb;
        public final Toolbar mToolbar;

        public ViewHolder(View itemView) {
            super(itemView);
            mThumb = (ImageView) itemView.findViewById(R.id.deck_thumb);
            mToolbar = (Toolbar) itemView.findViewById(R.id.deck_card_toolbar);
        }
    }
}
