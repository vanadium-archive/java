// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.moments.lib;

import android.os.Handler;

import io.v.moments.ifc.AdConverter;
import io.v.moments.ifc.HasId;
import io.v.moments.ifc.IdSet;
import io.v.moments.ifc.ScanListener;
import io.v.v23.discovery.Service;
import io.v.v23.discovery.Update;

/**
 * List that updates itself in response to found or lost advertisements.
 */
public class DiscoveredList<T extends HasId> extends ObservedList<T> implements ScanListener {
    private final String TAG = "DiscoveredList";

    private final Handler mHandler;

    // Converts a service description from an advertisement to an instance of T.
    private final AdConverter<T> mConverter;

    // Ids to ignore (likely local advertisements).
    private final IdSet mRejects;

    public DiscoveredList(
            AdConverter<T> converter, IdSet rejects, Handler handler) {
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

    @Override
    public void scanUpdateReceived(Update update) {
        if (update instanceof Update.Found) {
            maybeInsertItem((Update.Found) update);
            return;
        }
        removeItem((Update.Lost) update);
    }

    /**
     * Accept the advertisement if it's not on the reject list.
     */
    private void maybeInsertItem(Update.Found found) {
        Service service = found.getElem().getService();
        final Id id = Id.fromString(service.getInstanceId());
        if (mRejects.contains(id)) {
            return;
        }
        final T item = mConverter.make(service);
        if (item == null) {
            return;
        }
        insertItem(id, item);
    }

    private void insertItem(final Id id, final T item) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                push(item);
            }
        });
    }

    private void removeItem(Update.Lost lost) {
        final Id id = Id.fromString(lost.getElem().getService().getInstanceId());
        mHandler.post(new Runnable() {
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
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                while (size() > 0) {
                    remove(size() - 1);
                }
            }
        });
    }
}
