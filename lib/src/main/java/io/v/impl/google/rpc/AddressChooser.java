// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.impl.google.rpc;

import io.v.v23.rpc.NetworkAddress;
import io.v.v23.verror.VException;

public class AddressChooser implements io.v.v23.rpc.AddressChooser {
    private final long nativePtr;

    private native NetworkAddress[] nativeChoose(long nativePtr,
            String protocol, NetworkAddress[] candidates) throws VException;
    private native void nativeFinalize(long nativePtr);

    private AddressChooser(long nativePtr) {
        this.nativePtr = nativePtr;
    }
    @Override
    public NetworkAddress[] choose(String protocol, NetworkAddress[] candidates) throws VException {
        return nativeChoose(this.nativePtr, protocol, candidates);
    }
    @Override
    protected void finalize() {
        nativeFinalize(this.nativePtr);
    }
}
