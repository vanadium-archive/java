// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.impl.google;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

import io.v.v23.rpc.Callback;
import io.v.v23.verror.VException;

/**
 * A {@link Callback} that creates a {@link ListenableFuture} whose success/failure depends
 * on success/failure of the callback.
 */
public class ListenableFutureCallback<T> implements Callback<T> {
    private final SettableFuture<T> future = SettableFuture.create();

    /**
     * Returns a {@link ListenableFuture} whose success/failure depends on success/failure of this
     * {@link Callback}.
     */
    public ListenableFuture<T> getFuture() {
        return future;
    }

    @Override
    public void onSuccess(T result) {
        future.set(result);
    }
    @Override
    public void onFailure(VException error) {
        future.setException(error);
    }
}