// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.syncslides;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.Toolbar;

/**
 * Provides a list of decks to be shown in the RecyclerView of the
 * DeckChooserFragment.
 */
public class DeckListAdapter extends RecyclerView.Adapter<DeckListAdapter.ViewHolder>
        implements DB.Listener {
    private static final String TAG = "DeckListAdapter";
    private DB.DeckList mDecks;

    public DeckListAdapter(DB db) {
        mDecks = db.getDecks();
        mDecks.setListener(this);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int i) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.deck_card, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int i) {
        DB.Deck deck = mDecks.getDeck(i);
        holder.mToolbar.setTitle(deck.getTitle());
        // TODO(kash): We need to say when the user last viewed the deck or show
        // that the deck is active.  Either use the subtitle for this or create
        // a custom view for both the title and subtitle.
        holder.mThumb.setImageBitmap(deck.getThumb());
    }

    @Override
    public int getItemCount() {
        return mDecks.getItemCount();
    }

    /**
     * Stops any background monitoring of the underlying data.
     */
    public void stop() {
        mDecks.discard();
        mDecks = null;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final ImageView mThumb;
        public final Toolbar mToolbar;

        public ViewHolder(final View itemView) {
            super(itemView);
            mThumb = (ImageView) itemView.findViewById(R.id.deck_thumb);
            mToolbar = (Toolbar) itemView.findViewById(R.id.deck_card_toolbar);
            mThumb.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final Context context = v.getContext();
                    // Intent for the activity to open when user selects the thumbnail.
                    Intent presentationIntent = new Intent(context, PresentationActivity.class);
                    context.startActivity(presentationIntent);
                }
            });
            mToolbar.inflateMenu(R.menu.deck_card);
            mToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    switch (item.getItemId()) {
                        case R.id.action_delete_deck:
                            // TODO(kash): Actually delete the deck.
                            Toast.makeText(mToolbar.getContext(), "Delete", Toast.LENGTH_SHORT)
                                    .show();
                            return true;
                    }
                    return false;
                }
            });
        }
    }
}