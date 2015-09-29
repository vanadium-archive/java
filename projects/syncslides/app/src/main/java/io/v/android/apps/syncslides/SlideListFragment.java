// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.syncslides;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class SlideListFragment extends Fragment {

    private static final String SLIDE_LIST_TITLE = "Pitch deck";
    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLayoutManager;
    private SlideListAdapter mAdapter;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_slide_list, container, false);

        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.slide_list);
        mRecyclerView.setHasFixedSize(true);

        mLayoutManager = new LinearLayoutManager(container.getContext(),
                LinearLayoutManager.VERTICAL, false);
        mRecyclerView.setLayoutManager(mLayoutManager);

        mAdapter = new SlideListAdapter();
        mRecyclerView.setAdapter(mAdapter);
        getActivity().setTitle(SLIDE_LIST_TITLE);
        return rootView;
    }
}