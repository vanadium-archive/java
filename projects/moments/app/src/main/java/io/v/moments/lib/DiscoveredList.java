// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.moments.lib;

import android.os.Handler;

import io.v.moments.ifc.HasId;
import io.v.moments.ifc.IdSet;
import io.v.moments.v23.ifc.AdConverter;
import io.v.moments.v23.ifc.AdvertisementFoundListener;
import io.v.moments.v23.ifc.AdvertisementLostListener;
import io.v.v23.discovery.Advertisement;

/**
 * List that updates itself in response to found or lost advertisements.
 */
public class DiscoveredList<T extends HasId> extends ObservedList<T>
        implements AdvertisementFoundListener, AdvertisementLostListener {
    private static final String TAG = "DiscoveredList";

    private final Handler mHandler;

    // Converts a service description from an advertisement to an instance of T.
    private final AdConverter<T> mConverter;

    // Ids to ignore (likely local advertisements).
    private final IdSet mRejects;

    public DiscoveredList(AdConverter<T> converter, IdSet rejects, Handler handler) {
        if (converter == null) {
            throw new IllegalArgumentException("Null converter");
        }
        if (rejects == null) {
            throw new IllegalArgumentException("Null rejects");
        }
        if (handler == null) {
            throw new IllegalArgumentException("Null handler");
        }
        mConverter = converter;
        mRejects = rejects;
        mHandler = handler;
    }

    /**
     * Accept the advertisement if it's not on the reject list.
     */
    @Override
    public void handleFoundAdvertisement(Advertisement advertisement) {
        final Id id = Id.fromAdId(advertisement.getId());
        if (mRejects.contains(id)) {
            return;
        }
        final T item = mConverter.make(advertisement);
        if (item == null) {
            return;
        }
        mHandler.post(
                new Runnable() {
                    @Override
                    public void run() {
                        push(item);
                    }
                });
    }

    /**
     * Remove the lost advertisement.
     */
    @Override
    public void handleLostAdvertisement(Advertisement advertisement) {
        final Id id = Id.fromAdId(advertisement.getId());
        mHandler.post(
                new Runnable() {
                    @Override
                    public void run() {
                        if (mRejects.contains(id)) {
                            return;
                        }
                        int i = findIndexWithId(id);
                        if (i == NOT_FOUND) {
                            return;
                        }
                        remove(i);
                    }
                });
    }

    public void dropAll() {
        mHandler.post(
                new Runnable() {
                    @Override
                    public void run() {
                        while (size() > 0) {
                            remove(size() - 1);
                        }
                    }
                });
    }
}
