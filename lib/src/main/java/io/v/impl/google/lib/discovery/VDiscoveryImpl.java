// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.impl.google.lib.discovery;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;

import io.v.impl.google.ListenableFutureCallback;
import io.v.v23.InputChannel;
import io.v.v23.context.VContext;
import io.v.v23.discovery.Service;
import io.v.v23.discovery.Update;
import io.v.v23.discovery.VDiscovery;
import io.v.v23.security.BlessingPattern;
import io.v.v23.verror.VException;

class VDiscoveryImpl implements VDiscovery {
    private long nativeDiscoveryPtr;
    private long nativeTriggerPtr;

    private native void nativeAdvertise(
            long nativeDiscoveryPtr, long nativeTriggerPtr, VContext ctx, Service service,
            List<BlessingPattern> visibility,
            ListenableFutureCallback<ListenableFuture<Void>> startCallback,
            ListenableFutureCallback<Void> doneCallback);
    private native InputChannel<Update> nativeScan(
            long nativeDiscoveryPtr, VContext ctx, String query) throws VException;
    private native void nativeFinalize(long nativeDiscoveryPtr, long nativeTriggerPtr);

    private VDiscoveryImpl(long nativeDiscoveryPtr, long nativeTriggerPtr) {
        this.nativeDiscoveryPtr = nativeDiscoveryPtr;
        this.nativeTriggerPtr = nativeTriggerPtr;
    }
    @Override
    public ListenableFuture<ListenableFuture<Void>> advertise(VContext ctx, Service service,
                                                              List<BlessingPattern> visibility) {
        ListenableFutureCallback<ListenableFuture<Void>> startCallback = new ListenableFutureCallback<>();
        ListenableFutureCallback<Void> doneCallback = new ListenableFutureCallback<>();
        nativeAdvertise(nativeDiscoveryPtr, nativeTriggerPtr, ctx, service, visibility,
                startCallback, doneCallback);
        return startCallback.getFuture(ctx);
    }
    @Override
    public InputChannel<Update> scan(VContext ctx, String query) {
        try {
            return nativeScan(nativeDiscoveryPtr, ctx, query);
        } catch (VException e) {
            throw new RuntimeException("Couldn't start discovery scan()", e);
        }
    }
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        nativeFinalize(nativeDiscoveryPtr, nativeTriggerPtr);
    }
}
