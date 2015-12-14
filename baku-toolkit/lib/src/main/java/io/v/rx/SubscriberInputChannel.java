// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.rx;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

import io.v.v23.InputChannel;
import io.v.v23.verror.EndOfFileException;
import lombok.Synchronized;
import rx.Subscriber;

public class SubscriberInputChannel<T> extends Subscriber<T> implements InputChannel<T> {
    private Queue<SettableFuture<T>> mRequestQueue = new ConcurrentLinkedDeque<>();

    @Override
    @Synchronized
    public ListenableFuture<T> recv() {
        final SettableFuture<T> request = SettableFuture.create();
        mRequestQueue.add(request);
        request(1);
        return request;
    }

    @Override
    public void onNext(final T t) {
        mRequestQueue.poll().set(t);
    }

    @Override
    public void onError(Throwable e) {
        while (!mRequestQueue.isEmpty()) {
            mRequestQueue.poll().setException(e);
        }
    }

    @Override
    public void onCompleted() {
        while (!mRequestQueue.isEmpty()) {
            mRequestQueue.poll().setException(new EndOfFileException(null));
        }
    }
}
