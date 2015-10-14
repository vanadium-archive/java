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

import io.v.android.apps.syncslides.db.DB;
import io.v.android.apps.syncslides.discovery.Discovery;
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
    private Discovery.DList mLivePresList;

    public DeckListAdapter(DB db, Discovery discovery) {
        mLivePresList = discovery.getLivePresentations();
        mDecks = db.getDecks();

        mLivePresList.setListener(new Listener() {
            @Override
            public void notifyItemChanged(int position) {
                DeckListAdapter.this.notifyItemChanged(position);
            }

            @Override
            public void notifyItemInserted(int position) {
                DeckListAdapter.this.notifyItemInserted(position);
            }

            @Override
            public void notifyItemRemoved(int position) {
                DeckListAdapter.this.notifyItemRemoved(position);
            }
        });

        mDecks.setListener(new Listener() {
            @Override
            public void notifyItemChanged(int position) {
                DeckListAdapter.this.notifyItemChanged(mLivePresList.getItemCount() + position);
            }

            @Override
            public void notifyItemInserted(int position) {
                DeckListAdapter.this.notifyItemInserted(mLivePresList.getItemCount() + position);
            }

            @Override
            public void notifyItemRemoved(int position) {
                DeckListAdapter.this.notifyItemRemoved(mLivePresList.getItemCount() + position);
            }
        });

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
        // If the position is less than the number of live presentation decks, get deck card from
        // there (and don't allow the user to delete the deck). If not, get the card from the DB.
        if (i < mLivePresList.getItemCount()) {
            deck = mLivePresList.get(i);
            holder.mToolbar.getMenu().clear();
            role = Role.AUDIENCE;
        } else {
            deck = mDecks.get(i - mLivePresList.getItemCount());
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
        // TODO(afergan): Display "LIVE NOW" subtitle in toolbar for live presentations.
        // TODO(kash): We need to say when the user last viewed the deck or show
        // that the deck is active.  Either use the subtitle for this or create
        // a custom view for both the title and subtitle.
        holder.mThumb.setImageBitmap(deck.getThumb());
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
        return mLivePresList.getItemCount() + mDecks.getItemCount();
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
            mToolbar.inflateMenu(R.menu.deck_card);
        }
    }
}