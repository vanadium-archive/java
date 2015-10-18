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
import android.widget.TextView;
import android.widget.Toolbar;
import io.v.android.apps.syncslides.db.DB;
import io.v.android.apps.syncslides.discovery.DiscoveryManager;
import io.v.android.apps.syncslides.model.Deck;
import io.v.android.apps.syncslides.model.Listener;
import io.v.android.apps.syncslides.model.Participant;

/**
 * Provides a list of decks to be shown in the RecyclerView of the
 * DeckChooserFragment.
 */
public class DeckListAdapter extends RecyclerView.Adapter<DeckListAdapter.ViewHolder>
        implements Listener {
    private static final String TAG = "DeckListAdapter";
    private DB.DBList<Deck> mDecks;
    private DB.DBList<Deck> mLiveDecks;
    private DB mDB;

    public DeckListAdapter(DB db) {
        mDB = db;
    }

    public void start(Context context) {
        if (mDecks != null) {
            throw new IllegalStateException("Wrong lifecycle.");
        }
        Log.d(TAG, "Starting.");
        DiscoveryManager dm = DiscoveryManager.Singleton.get();
        dm.setListener(this);
        dm.start(context);
        mLiveDecks = dm;
        mDecks = DB.Singleton.get(context).getDecks();
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
        // If the position is less than the number of live presentation decks, get deck card from
        // there (and don't allow the user to delete the deck). If not, get the card from the DB.
        if (i < mLiveDecks.getItemCount()) {
            deck = mLiveDecks.get(i);
            holder.mToolbarLiveNow
                    .setText(holder.itemView.getResources().getString(R.string.presentation_live));
            holder.mToolbarLiveNow.setVisibility(View.VISIBLE);
            holder.mToolbarLastOpened.setVisibility(View.GONE);
            holder.mToolbar.getMenu().clear();
            role = Role.AUDIENCE;
        } else {
            deck = mDecks.get(i - mLiveDecks.getItemCount());
            // TODO(afergan): Set actual date here.
            holder.mToolbarLastOpened.setText("Opened on Oct 26, 2015");
            holder.mToolbarLastOpened.setVisibility(View.VISIBLE);
            holder.mToolbarLiveNow.setVisibility(View.GONE);
            holder.mToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    switch (item.getItemId()) {
                        case R.id.action_delete_deck:
                            mDB.deleteDeck(deck.getId());
                            return true;
                    }
                    return false;
                }
            });
            role = Role.BROWSER;
        }
        holder.mToolbarTitle.setText(deck.getTitle());
        // TODO(kash): We need to say when the user last viewed the deck.
        Bitmap thumb = deck.getThumb();
        if (thumb == null) {
            thumb = makeDefaultThumb(holder.mToolbar.getContext());
        }
        holder.mThumb.setImageBitmap(thumb);
        holder.mThumb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Clicking through to PresentationActivity.");
                final Context context = v.getContext();
                if (role == Role.AUDIENCE) {
                    //String deviceId = "355499060906851";  // Black Nexus 6.
                    String deviceId = "355499060490393"; // Nexus 6 ZX1G22MLNL
                    DB.Singleton.get(context).joinPresentation(
                            // TODO(kash): Use the real syncgroup name.
                            "/192.168.86.254:8101/"+deviceId+"/%%sync/syncslides/" +
                                    "deckId1/randomPresentationId1",
                            new DB.Callback<Void>() {
                                @Override
                                public void done(Void aVoid) {
                                    // Intent for the activity to open when user selects the thumbnail.
                                    Intent intent = new Intent(context, PresentationActivity.class);
                                    intent.putExtras(deck.toBundle(null));
                                    intent.putExtra(Participant.B.PARTICIPANT_ROLE, role);
                                    context.startActivity(intent);
                                }
                            });
                } else {
                    // Intent for the activity to open when user selects the thumbnail.
                    Intent intent = new Intent(context, PresentationActivity.class);
                    intent.putExtras(deck.toBundle(null));
                    intent.putExtra(Participant.B.PARTICIPANT_ROLE, role);
                    context.startActivity(intent);
                }
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
        public final TextView mToolbarTitle;
        public final TextView mToolbarLiveNow;
        public final TextView mToolbarLastOpened;

        public ViewHolder(final View itemView) {
            super(itemView);
            mThumb = (ImageView) itemView.findViewById(R.id.deck_thumb);
            mToolbar = (Toolbar) itemView.findViewById(R.id.deck_card_toolbar);
            mToolbarTitle = (TextView) itemView.findViewById(R.id.deck_card_toolbar_title);
            mToolbarLiveNow = (TextView) itemView.findViewById(R.id.deck_card_toolbar_live_now);
            mToolbarLastOpened =
                    (TextView) itemView.findViewById(R.id.deck_card_toolbar_last_opened);
            mToolbar.inflateMenu(R.menu.deck_card);
        }
    }
}
