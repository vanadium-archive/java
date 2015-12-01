// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.impl.google.channel;

import com.google.common.util.concurrent.ListenableFuture;

import io.v.impl.google.ListenableFutureCallback;
import io.v.v23.OutputChannel;
import io.v.v23.rpc.Callback;

/**
 * An implementation of {@link OutputChannel} that sends data using Go send, convert, and close
 * functions.
 */
class OutputChannelImpl<T> implements OutputChannel<T> {
    private final long nativeConvertPtr;
    private final long nativeSendPtr;
    private final long nativeClosePtr;

    private static native <T> void nativeSend(long nativeConvertPtr, long nativeSendPtr, T value,
                                              Callback<Void> callback);
    private static native void nativeClose(long nativeClosePtr, Callback<Void> callback);
    private static native void nativeFinalize(long nativeConvertPtr, long nativeSendPtr, long nativeClosePtr);

    private OutputChannelImpl(long convertPtr, long sendPtr, long closePtr) {
        this.nativeConvertPtr = convertPtr;
        this.nativeSendPtr = sendPtr;
        this.nativeClosePtr = closePtr;
    }
    @Override
    public ListenableFuture<Void> send(T item) {
        ListenableFutureCallback<Void> callback = new ListenableFutureCallback<>();
        nativeSend(nativeConvertPtr, nativeSendPtr, item, callback);
        return callback.getFuture();
    }
    @Override
    public ListenableFuture<Void> close() {
        ListenableFutureCallback<Void> callback = new ListenableFutureCallback<>();
        nativeClose(nativeClosePtr, callback);
        return callback.getFuture();
    }
    @Override
    protected void finalize() throws Throwable {
        nativeFinalize(nativeConvertPtr, nativeSendPtr, nativeClosePtr);
    }
}
