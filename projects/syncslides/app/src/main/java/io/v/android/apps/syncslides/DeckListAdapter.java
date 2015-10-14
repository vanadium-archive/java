// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.syncslides;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.Toolbar;

import io.v.android.apps.syncslides.db.DB;
import io.v.android.apps.syncslides.discovery.DiscoveryManager;
import io.v.android.apps.syncslides.model.Deck;
import io.v.android.apps.syncslides.model.Listener;

/**
 * Provides a list of decks to be shown in the RecyclerView of the
 * DeckChooserFragment.
 */
public class DeckListAdapter extends RecyclerView.Adapter<DeckListAdapter.ViewHolder>
        implements Listener {
    private static final String TAG = "DeckListAdapter";
    private DB.DBList<Deck> mDecks;
    private DB.DBList<Deck> mLiveDecks;

    public void start(DiscoveryManager discoveryManager, DB.DBList<Deck> decks) {
        if (mDecks != null) {
            throw new IllegalStateException("Wrong lifecycle.");
        }
        Log.d(TAG, "Starting.");
        mLiveDecks = discoveryManager;
        mLiveDecks.setListener(this);
        discoveryManager.start();
        mDecks = decks;
        mDecks.setListener(new Listener() {
            @Override
            public void notifyItemChanged(int position) {
                DeckListAdapter.this.notifyItemChanged(mLiveDecks.getItemCount() + position);
            }

            @Override
            public void notifyItemInserted(int position) {
                DeckListAdapter.this.notifyItemInserted(mLiveDecks.getItemCount() + position);
            }

            @Override
            public void notifyItemRemoved(int position) {
                DeckListAdapter.this.notifyItemRemoved(mLiveDecks.getItemCount() + position);
            }
        });
    }

    /**
     * Stops any background monitoring of the underlying data.
     */
    public void stop() {
        Log.d(TAG, "Stopping.");
        mLiveDecks.discard();
        mLiveDecks = null;
        mDecks.discard();
        mDecks = null;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int i) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.deck_card, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int i) {
        final Deck deck;
        final Role role;
        final boolean isLive;

        // If the position is less than the number of live presentation decks, get deck card from
        // there (and don't allow the user to delete the deck). If not, get the card from the DB.
        if (i < mLiveDecks.getItemCount()) {
            isLive = true;
            deck = mLiveDecks.get(i);
            holder.mToolbar.getMenu().clear();
            role = Role.AUDIENCE;
        } else {
            isLive = false;
            deck = mDecks.get(i - mLiveDecks.getItemCount());
            holder.mToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    switch (item.getItemId()) {
                        case R.id.action_delete_deck:
                            // TODO(kash): Actually delete the deck.
                            Toast.makeText(
                                    holder.mToolbar.getContext(), "Delete", Toast.LENGTH_SHORT)
                                    .show();
                            return true;
                    }
                    return false;
                }
            });
            role = Role.BROWSER;
        }

        holder.mToolbar.setTitle(deck.getTitle());
        // TODO(kash): We need to say when the user last viewed the deck.
        Bitmap thumb = deck.getThumb();
        if (thumb == null) {
            thumb = makeDefaultThumb(holder.mToolbar.getContext());
        }
        holder.mThumb.setImageBitmap(thumb);
        if (isLive) {
            // TODO(afergan): Display "LIVE NOW" subtitle in toolbar for live presentations.
            holder.mToolbar.setTitle(" " + deck.getTitle());
            holder.mToolbar.setLogo(R.drawable.orange_circle);
        }

        holder.mThumb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Context context = v.getContext();
                // Intent for the activity to open when user selects the thumbnail.
                Intent intent = new Intent(context, PresentationActivity.class);
                intent.putExtra(PresentationActivity.DECK_ID_KEY, deck.getId());
                intent.putExtra(PresentationActivity.ROLE_KEY, role);
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mLiveDecks.getItemCount() + mDecks.getItemCount();
    }

    private Bitmap makeDefaultThumb(Context c) {
        return BitmapFactory.decodeResource(
                c.getResources(), R.drawable.thumb_deck3);
    }
    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final ImageView mThumb;
        public final Toolbar mToolbar;

        public ViewHolder(final View itemView) {
            super(itemView);
            mThumb = (ImageView) itemView.findViewById(R.id.deck_thumb);
            mToolbar = (Toolbar) itemView.findViewById(R.id.deck_card_toolbar);
            mToolbar.inflateMenu(R.menu.deck_card);
        }
    }
}
