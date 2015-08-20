// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.namespace_browser;

import com.google.common.reflect.TypeToken;

import org.joda.time.Duration;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import io.v.android.v23.V;
import io.v.v23.context.VContext;
import io.v.v23.rpc.Client;
import io.v.v23.rpc.ClientCall;
import io.v.v23.vdlroot.signature.Interface;
import io.v.v23.vdlroot.signature.Method;
import io.v.v23.verror.VException;

/**
 * Namespace provides utility methods for working with Veyron object methods.
 */
public class Methods {
    /**
     * Returns the list of method names of the provided object.
     *
     * @param name a name of the object
     * @return list of method names of the provided object.
     * @throws VException if there was an error getting the list of names.
     */
    public static List<String> get(String name, VContext ctx) throws VException {
        Client client = V.getClient(ctx);
        VContext ctxT = ctx.withTimeout(new Duration(20000)); // 20s
        ClientCall call =
                client.startCall(ctxT, name, "__Signature", new Object[0], new Type[0]);
        Type[] resultTypes = new Type[]{new TypeToken<Interface[]>() {
        }.getType()};
        Interface[] sSign = (Interface[]) call.finish(resultTypes)[0];
        List<String> ret = new ArrayList<String>();
        for (Interface iface : sSign) {
            if (iface.getMethods() != null) {
                for (Method method : iface.getMethods()) {
                    ret.add(method.getName());
                }
            }
        }
        return ret;
    }
}
