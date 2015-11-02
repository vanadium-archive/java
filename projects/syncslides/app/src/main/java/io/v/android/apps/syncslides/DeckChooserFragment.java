// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.syncslides;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
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

import com.google.common.base.Charsets;
import com.google.common.io.ByteStreams;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.UUID;

import io.v.android.apps.syncslides.db.DB;
import io.v.android.apps.syncslides.model.Deck;
import io.v.android.apps.syncslides.model.DeckFactory;
import io.v.android.apps.syncslides.model.Slide;
import io.v.android.apps.syncslides.model.SlideImpl;

/**
 * This fragment contains the list of decks as well as the FAB to create a new
 * deck.
 */
public class DeckChooserFragment extends Fragment {
    /**
     * The fragment argument representing the section number for this fragment.
     */
    private static final String ARG_SECTION_NUMBER = "section_number";
    private static final String TAG = "DeckChooserFragment";
    private static final int REQUEST_CODE_IMPORT_DECK = 1000;
    private RecyclerView mRecyclerView;
    private GridLayoutManager mLayoutManager;
    private DeckListAdapter mAdapter;

    /**
     * Returns a new instance of this fragment for the given section number.
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
                    toast("Error selecting deck to import " + errorStr);
                    break;
                }
                Uri uri = data.getData();
                importDeck(DocumentFile.fromTreeUri(getContext(), uri));
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
        Log.i(TAG, "Starting");
        DB db = DB.Singleton.get(getActivity().getApplicationContext());
        mAdapter = new DeckListAdapter(db);
        mAdapter.start(getActivity().getApplicationContext());
        mRecyclerView.setAdapter(mAdapter);
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.i(TAG, "Stopping");
        mAdapter.stop();
        mAdapter = null;
    }

    /**
     * Import a deck so it shows up in the list of all decks.
     */
    private void onImportDeck() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        startActivityForResult(intent, REQUEST_CODE_IMPORT_DECK);
    }

    /**
     * Import a slide deck from the given (local) folder.
     *
     * The folder must contain a JSON metadata file 'deck.json' with the following format:
     * {
     *     "Title" : "<title>",
     *     "Thumb" : "<filename>,
     *     "Slides" : [
     *          {
     *              "Thumb" : "<thumb_filename1>",
     *              "Image" : "<image_filename1>",
     *              "Note" : "<note1>"
     *          },
     *          {
     *              "Thumb" : "<thumb_filename2>",
     *              "Image" : "<image_filename2>",
     *              "Note" : "<note2>"
     *          },
     *
     *          ...
     *     ]
     * }
     *
     * All the filenames must be local to the given folder.
     */
    private void importDeck(DocumentFile dir) {
        if (!dir.isDirectory()) {
            toast("Must import from a directory, got: " + dir);
            return;
        }
        // Read the deck metadata file.
        DocumentFile metadataFile = dir.findFile("deck.json");
        if (metadataFile == null) {
            toast("Couldn't find deck metadata file 'deck.json'");
            return;
        }
        JSONObject metadata = null;
        try {
            String data = new String(ByteStreams.toByteArray(
                    getActivity().getContentResolver().openInputStream(metadataFile.getUri())),
                    Charsets.UTF_8);
            metadata = new JSONObject(data);
        } catch (FileNotFoundException e) {
            toast("Couldn't open deck metadata file: " + e.getMessage());
            return;
        } catch (IOException e) {
            toast("Couldn't read data from deck metadata file: " + e.getMessage());
            return;
        } catch (JSONException e) {
            toast("Couldn't parse deck metadata: " + e.getMessage());
            return;
        }

        try {
            String id = UUID.randomUUID().toString();
            String title = metadata.getString("Title");
            Bitmap thumb = readImage(dir, metadata.getString("Thumb"));
            Deck deck = DeckFactory.Singleton.get().make(title, thumb, id);
            Slide[] slides = readSlides(dir, metadata);
            // TODO(spetrovic): Do this asynchronously.
            DB.Singleton.get(getActivity().getApplicationContext()).importDeck(deck, slides);
        } catch (JSONException e) {
            toast("Invalid format for deck metadata: " + e.getMessage());
            return;
        } catch (IOException e) {
            toast("Error interpreting deck metadata: " + e.getMessage());
            return;
        }
    }

    private Slide[] readSlides(DocumentFile dir, JSONObject metadata)
            throws JSONException, IOException {
        if (!metadata.has("Slides")) {
            return new Slide[0];
        }
        JSONArray slides = metadata.getJSONArray("Slides");
        Slide[] ret = new Slide[slides.length()];
        for (int i = 0; i < slides.length(); ++i) {
            JSONObject slide = slides.getJSONObject(i);
            // TODO(jregan): Avoid the extra image conversion work.
            // Reading into a bitmap, only to compress into bytes again.
            byte[] thumbData =
                    DeckFactory.makeBytesFromBitmap(readImage(dir, slide.getString("Thumb")));
            byte[] imageData = thumbData;
            if (slide.has("Image")) {
                imageData =
                        DeckFactory.makeBytesFromBitmap(readImage(dir, slide.getString("Image")));
            }
            String note = slide.getString("Note");
            ret[i] = new SlideImpl(thumbData, imageData, note);
        }
        return ret;
    }

    private Bitmap readImage(DocumentFile dir, String fileName) throws IOException {
        DocumentFile file = dir.findFile(fileName);
        if (file == null) {
            throw new FileNotFoundException(
                    "Thumbnail file doesn't exist: " + fileName);
        }
        return MediaStore.Images.Media.getBitmap(
                getActivity().getContentResolver(), file.getUri());
    }

    private void toast(String msg) {
        Toast.makeText(getActivity(), msg, Toast.LENGTH_LONG).show();
    }
}
