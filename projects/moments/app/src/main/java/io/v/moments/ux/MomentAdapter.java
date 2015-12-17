// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.moments.ux;

import android.content.Context;
import android.os.Handler;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.concurrent.ExecutorService;

import io.v.moments.R;
import io.v.moments.ifc.ListObserver;
import io.v.moments.ifc.Moment;
import io.v.moments.ifc.Moment.Kind;
import io.v.moments.lib.ObservedList;
import io.v.moments.model.AdvertiserFactory;

/**
 * Stacks two moment lists in a recycler view.
 */
public class MomentAdapter extends RecyclerView.Adapter<MomentHolder>
        implements ListObserver {
    private final ObservedList<Moment> mRemoteMoments;
    private final ObservedList<Moment> mLocalMoments;
    private final AdvertiserFactory mAdvertiserFactory;
    private final ExecutorService mExecutor;
    private final Handler mHandler;

    public MomentAdapter(ObservedList<Moment> remoteMoments,
                         ObservedList<Moment> localMoments,
                         AdvertiserFactory advertiserFactory,
                         ExecutorService executor,
                         Handler handler) {
        mRemoteMoments = remoteMoments;
        mLocalMoments = localMoments;
        mAdvertiserFactory = advertiserFactory;
        mExecutor = executor;
        mHandler = handler;
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
        return new MomentHolder(view, context, mExecutor, mHandler);
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
        if (isRemote(position)) {
            holder.bind(moment, Kind.REMOTE, null);
        } else {
            holder.bind(moment, Kind.LOCAL, mAdvertiserFactory.make(moment));
        }
    }

    @Override
    public int getItemCount() {
        return mRemoteMoments.size() + mLocalMoments.size();
    }
}
