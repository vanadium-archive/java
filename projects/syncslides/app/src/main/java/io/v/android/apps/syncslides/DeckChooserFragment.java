// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.syncslides;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.provider.DocumentFile;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Toast;

import io.v.android.apps.syncslides.db.DB;

/**
 * This fragment contains the list of decks as well as the FAB to create a new
 * deck.
 */
public class DeckChooserFragment extends Fragment {
    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private static final String ARG_SECTION_NUMBER = "section_number";
    private static final String TAG = "ChooserFragment";
    private static final int REQUEST_CODE_IMPORT_DECK = 1000;
    private RecyclerView mRecyclerView;
    private GridLayoutManager mLayoutManager;
    private DeckListAdapter mAdapter;

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static DeckChooserFragment newInstance(int sectionNumber) {
        DeckChooserFragment fragment = new DeckChooserFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_deck_chooser, container, false);
        FloatingActionButton fab = (FloatingActionButton) rootView.findViewById(R.id.new_deck_fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onImportDeck();
            }
        });

        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.deck_grid);
        mRecyclerView.setHasFixedSize(true);

        // Statically set the span count (i.e. number of columns) for now...  See below.
        mLayoutManager = new GridLayoutManager(getContext(), 2);
        mRecyclerView.setLayoutManager(mLayoutManager);
        // Dynamically set the span based on the screen width.  Cribbed from
        // http://stackoverflow.com/questions/26666143/recyclerview-gridlayoutmanager-how-to-auto-detect-span-count
        mRecyclerView.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        mRecyclerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        int viewWidth = mRecyclerView.getMeasuredWidth();
                        float cardViewWidth = getActivity().getResources().getDimension(
                                R.dimen.deck_card_width);
                        int newSpanCount = (int) Math.floor(viewWidth / cardViewWidth);
                        mLayoutManager.setSpanCount(newSpanCount);
                        mLayoutManager.requestLayout();
                    }
                });

        return rootView;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_IMPORT_DECK:
                if (resultCode != Activity.RESULT_OK) {
                    String errorStr = data != null && data.hasExtra(DocumentsContract.EXTRA_ERROR)
                            ? data.getStringExtra(DocumentsContract.EXTRA_ERROR)
                            : "";
                    Toast.makeText(getActivity(), "Error selecting deck to import " + errorStr,
                            Toast.LENGTH_LONG).show();
                    break;
                }
                DocumentFile pickedDir = DocumentFile.fromTreeUri(getContext(), data.getData());
                Log.i(TAG, "Picked folder: " + pickedDir.getUri());
                break;
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((DeckChooserActivity) activity).onSectionAttached(
                getArguments().getInt(ARG_SECTION_NUMBER));
    }

    @Override
    public void onStart() {
        super.onStart();
        DB db = DB.Singleton.get(getActivity().getApplicationContext());
        mAdapter = new DeckListAdapter(db);
        mRecyclerView.setAdapter(mAdapter);
    }

    @Override
    public void onStop() {
        super.onStop();
        mAdapter.stop();
        mAdapter = null;
    }

    /**
     * Import a deck so it shows up in the list of all decks.
     */
    private void onImportDeck() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, REQUEST_CODE_IMPORT_DECK);
    }
}