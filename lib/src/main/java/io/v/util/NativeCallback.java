// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.util;

import io.v.v23.rpc.Callback;
import io.v.v23.verror.VException;

/**
 * A {@link Callback} that calls native functions to handle success/failures.
 */
class NativeCallback<T> implements Callback<T> {
    private long nativeSuccessPtr;
    private long nativeFailurePtr;

    private native void nativeOnSuccess(long nativeSuccessPtr, T result);
    private native void nativeOnFailure(long nativeFailurePtr, VException error);
    private native void nativeFinalize(long nativeSuccessPtr, long nativeFailurePtr);

    private NativeCallback(long nativeSuccessPtr, long nativeFailurePtr) {
        this.nativeSuccessPtr = nativeSuccessPtr;
        this.nativeFailurePtr = nativeFailurePtr;
    }
    @Override
    public void onSuccess(T result) {
        nativeOnSuccess(nativeSuccessPtr, result);
    }
    @Override
    public void onFailure(VException error) {
        nativeOnFailure(nativeFailurePtr, error);
    }
    @Override
    protected void finalize() {
        nativeFinalize(nativeSuccessPtr, nativeFailurePtr);
    }
}
