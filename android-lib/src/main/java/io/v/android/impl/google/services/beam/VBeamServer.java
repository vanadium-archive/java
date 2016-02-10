// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.impl.google.services.beam;

import android.util.Pair;

import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import io.v.android.v23.VBeam;
import io.v.v23.context.VContext;
import io.v.v23.rpc.ServerCall;
import io.v.v23.verror.VException;

class VBeamServer implements IntentBeamerServer {
    private static final long EXPIRATION_MS = 60000;

    private final Map<String, Long> requestMap = new LinkedHashMap<>();
    private final VBeam.IntentCreator callback;

    public VBeamServer(VBeam.IntentCreator creator) throws VException {
        this.callback = creator;
    }

    @Override
    public synchronized ListenableFuture<GetIntentOut> getIntent(
            VContext ctx, ServerCall call, String secret) {
        evictStaleRequests();
        if (requestMap.remove(secret) != null) {
            try {
                return Futures.transform(
                        callback.createIntent(ctx, call),
                        new AsyncFunction<Pair<String, byte[]>, GetIntentOut>() {

                        @Override
                        public ListenableFuture<GetIntentOut> apply(Pair<String, byte[]> input)
                                throws Exception {
                            return convertIntent(input);
                        }
                });
            } catch (Throwable t) {
                return Futures.immediateFailedFuture(t);
            }
        }
        return Futures.immediateFailedFuture(new VException("Bad request"));
    }

    private static ListenableFuture<GetIntentOut> convertIntent(Pair<String, byte[]> intent) {
        GetIntentOut out = new GetIntentOut();
        out.intentUri = intent.first;
        out.payload = intent.second;
        return Futures.immediateFuture(out);
    }

    private synchronized void evictStaleRequests() {
        long now = System.currentTimeMillis();
        for (Iterator<Long> it = requestMap.values().iterator(); it.hasNext();) {
            long l = it.next();
            if (l > now || l < now - EXPIRATION_MS) {
                it.remove();
            } else {
                // LinkedHashMap is sorted by insertion order, so oldest entry is first.
                break;
            }
        }
    }

    synchronized String newRequest() {
        evictStaleRequests();
        String secret = UUID.randomUUID().toString();
        synchronized(requestMap) {
            requestMap.put(secret, System.currentTimeMillis());
        }
        return secret;
    }
}
