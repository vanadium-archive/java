// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.impl.google.channel;

import com.google.common.util.concurrent.ListenableFuture;

import io.v.impl.google.ListenableFutureCallback;
import io.v.v23.InputChannel;
import io.v.v23.context.VContext;
import io.v.v23.rpc.Callback;

/**
 * An implementation of {@link InputChannel} that reads data using Go recv function.
 */
class InputChannelImpl<T> implements InputChannel<T> {
    private final VContext ctx;
    private final long nativeRecvPtr;

    private native void nativeRecv(long nativeRecvPtr, Callback<T> callback);
    private native void nativeFinalize(long nativeRecvPtr);

    private InputChannelImpl(VContext ctx, long nativeRecvPtr) {
        this.ctx = ctx;
        this.nativeRecvPtr = nativeRecvPtr;
    }
    @Override
    public ListenableFuture<T> recv() {
        ListenableFutureCallback<T> callback = new ListenableFutureCallback<>();
        nativeRecv(nativeRecvPtr, callback);
        return callback.getFuture(ctx);
    }
    @Override
    protected void finalize() {
        nativeFinalize(nativeRecvPtr);
    }
}