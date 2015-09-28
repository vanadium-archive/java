// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.syncslides;

import android.app.Activity;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * This fragment contains the list of presentations as well as the FAB to create a new
 * presentation.
 */
public class PresentationChooserFragment extends Fragment {
    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private static final String ARG_SECTION_NUMBER = "section_number";
    private static final String TAG = "ChooserFragment";
    private RecyclerView mRecyclerView;
    private GridLayoutManager mLayoutManager;
    private PresentationListAdapter mAdapter;

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static PresentationChooserFragment newInstance(int sectionNumber) {
        PresentationChooserFragment fragment = new PresentationChooserFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_presentation_chooser, container,
                false);
        FloatingActionButton fab = (FloatingActionButton) rootView.findViewById(
                R.id.new_presentation_fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                newPresentation();
            }
        });

        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.presentation_grid);
        mRecyclerView.setHasFixedSize(true);

        // TODO(kash): Dynamically set the span based on the screen width.
        mLayoutManager = new GridLayoutManager(getContext(), 2);
        mRecyclerView.setLayoutManager(mLayoutManager);

        mAdapter = new PresentationListAdapter();
        mRecyclerView.setAdapter(mAdapter);
        return rootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((PresentationChooserActivity) activity).onSectionAttached(
                getArguments().getInt(ARG_SECTION_NUMBER));
    }

    /**
     * Import a presentation so it shows up in the list of all presentations.
     */
    private void newPresentation() {
        // TODO(afergan): Hook up new presentation screen here.
        Log.i(TAG, "newPresentation");
    }

}
