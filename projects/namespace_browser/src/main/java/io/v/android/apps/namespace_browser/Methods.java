// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.namespace_browser;

import com.google.common.base.Function;
import com.google.common.reflect.TypeToken;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.joda.time.Duration;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import io.v.android.v23.V;
import io.v.v23.context.VContext;
import io.v.v23.rpc.Client;
import io.v.v23.rpc.ClientCall;
import io.v.v23.vdlroot.signature.Interface;
import io.v.v23.vdlroot.signature.Method;
import io.v.v23.verror.VException;

class Methods {
    // Returns a new future whose result is the list of method names of the provided object.
    static ListenableFuture<List<String>> get(VContext ctx, String name) {
        Client client = V.getClient(ctx);
        VContext ctxT = ctx.withTimeout(new Duration(20000)); // 20s
        ListenableFuture<ClientCall> callFuture =
                client.startCall(ctxT, name, "__Signature", new Object[0], new Type[0]);
        final Type[] resultTypes = new Type[]{new TypeToken<Interface[]>() {
        }.getType()};
        ListenableFuture<Interface[]> sign = Futures.transform(callFuture,
                new AsyncFunction<ClientCall, Interface[]>() {
                    @Override
                    public ListenableFuture<Interface[]> apply(ClientCall call) throws Exception {
                        return Futures.transform(call.finish(resultTypes),
                                new Function<Object[], Interface[]>() {
                                    @Override
                                    public Interface[] apply(Object[] input) {
                                        return (Interface[]) (input[0]);
                                    }
                                });
                    }
                });
        return Futures.transform(sign, new Function<Interface[], List<String>>() {
            @Override
            public List<String> apply(Interface[] input) {
                List<String> ret = new ArrayList<String>();
                for (Interface iface : input) {
                    if (iface.getMethods() != null) {
                        for (Method method : iface.getMethods()) {
                            ret.add(method.getName());
                        }
                    }
                }
                return ret;
            }
        });
    }
}
