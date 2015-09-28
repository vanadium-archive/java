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
 * Provides a list of presentations to be shown in the RecyclerView of the
 * PresentationChooserFragment.
 */
public class PresentationListAdapter
        extends RecyclerView.Adapter<PresentationListAdapter.ViewHolder> {
    // TODO(kash): Replace this static data with syncbase.
    private static final int[] THUMBS = {
            R.drawable.thumb_presentation1,
            R.drawable.thumb_presentation2,
            R.drawable.thumb_presentation3
    };
    private static final String[] TITLES = {"Presentation 1", "Presentation 2", "Presentation 3"};


    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int i) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.presentation_card, parent, false);
        // TODO(kash): Add a menu that allows the user to delete a presentation.
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int i) {
        holder.mToolbar.setTitle(TITLES[i]);
        // TODO(kash): We need to say when the user last viewed the presentation or show
        // that the presentation is active.  Either use the subtitle for this or create
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
            mThumb = (ImageView) itemView.findViewById(R.id.presentation_thumb);
            mToolbar = (Toolbar) itemView.findViewById(R.id.presentation_card_toolbar);
        }
    }
}
