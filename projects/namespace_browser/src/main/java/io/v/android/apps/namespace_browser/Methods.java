// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.namespace_browser;

import com.google.common.reflect.TypeToken;

import org.joda.time.Duration;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import io.v.v23.android.V;
import io.v.v23.context.VContext;
import io.v.v23.rpc.Client;
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
        final Client client = V.getClient(ctx);
        final VContext ctxT = ctx.withTimeout(new Duration(20000)); // 20s
        final Client.Call call = client.startCall(ctxT, name, "signature", new Object[0], new Type[0]);
        final Type[] resultTypes = new Type[]{new TypeToken<Interface>() {
        }.getType()};
        final Interface sSign = (Interface) call.finish(resultTypes)[0];
        final List<String> ret = new ArrayList<String>();
        if (sSign.getMethods() != null) {
            for (Method method : sSign.getMethods()) {
                ret.add(method.getName());
            }
        }
        return ret;
    }
}
