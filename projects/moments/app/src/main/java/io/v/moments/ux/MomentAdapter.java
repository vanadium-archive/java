// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.moments.ux;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import io.v.moments.R;
import io.v.moments.ifc.ListObserver;
import io.v.moments.ifc.Moment;
import io.v.moments.lib.ObservedList;
import io.v.moments.model.AdvertiserFactory;
import io.v.moments.model.Toaster;

/**
 * Stacks two moment lists in a recycler view.
 */
class MomentAdapter extends RecyclerView.Adapter<MomentHolder>
        implements ListObserver {
    private final ObservedList<Moment> mRemoteMoments;
    private final ObservedList<Moment> mLocalMoments;
    private final Toaster mToaster;
    private final AdvertiserFactory mAdvertiserFactory;

    public MomentAdapter(ObservedList<Moment> remoteMoments,
                         ObservedList<Moment> localMoments,
                         Toaster toaster,
                         AdvertiserFactory advertiserFactory) {
        mRemoteMoments = remoteMoments;
        mLocalMoments = localMoments;
        mToaster = toaster;
        mAdvertiserFactory = advertiserFactory;
    }

    public void beginObserving() {
        mRemoteMoments.setObserver(this);
        mLocalMoments.setObserver(new ListObserver() {
            @Override
            public void notifyItemInserted(int position) {
                MomentAdapter.this.notifyItemInserted(mRemoteMoments.size() + position);
            }

            @Override
            public void notifyItemChanged(int position) {
                MomentAdapter.this.notifyItemChanged(mRemoteMoments.size() + position);
            }

            @Override
            public void notifyItemRemoved(int position) {
                MomentAdapter.this.notifyItemRemoved(mRemoteMoments.size() + position);
            }
        });
    }

    @Override
    public long getItemId(int position) {
        return getMoment(position).getId().toLong();
    }

    @Override
    public MomentHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.item_moment, parent, false);
        return new MomentHolder(view, context, mToaster);
    }

    private boolean isRemote(int position) {
        return position < mRemoteMoments.size();
    }

    private Moment getMoment(int position) {
        return isRemote(position) ?
                mRemoteMoments.get(position) :
                mLocalMoments.get(position - mRemoteMoments.size());
    }

    @Override
    public void onBindViewHolder(MomentHolder holder, int position) {
        Moment moment = getMoment(position);
        holder.bind(
                moment,
                isRemote(position) ? null : mAdvertiserFactory.getOrMake(moment));
    }

    @Override
    public int getItemCount() {
        return mRemoteMoments.size() + mLocalMoments.size();
    }
}
