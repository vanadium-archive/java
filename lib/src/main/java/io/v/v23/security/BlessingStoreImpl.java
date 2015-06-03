// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.security;

import io.v.v23.verror.VException;

import java.security.interfaces.ECPublicKey;
import java.util.Map;

class BlessingStoreImpl implements BlessingStore {
    private final long nativePtr;

    private native Blessings nativeSet(
        long nativePtr, Blessings blessings, BlessingPattern forPeers) throws VException;
    private native Blessings nativeForPeer(long nativePtr, String[] peerBlessings)
            throws VException;
    private native void nativeSetDefaultBlessings(long nativePtr, Blessings blessings)
            throws VException;
    private native Blessings nativeDefaultBlessings(long nativePtr) throws VException;
    private native ECPublicKey nativePublicKey(long nativePtr) throws VException;
    private native Map<BlessingPattern, Blessings> nativePeerBlessings(long nativePtr)
            throws VException;
    private native String nativeDebugString(long nativePtr);
    private native String nativeToString(long nativePtr);
    private native void nativeFinalize(long nativePtr);

    private BlessingStoreImpl(long nativePtr) {
        this.nativePtr = nativePtr;
    }

    @Override
    public Blessings set(Blessings blessings, BlessingPattern forPeers) throws VException {
        return nativeSet(this.nativePtr, blessings, forPeers);
    }
    @Override
    public Blessings forPeer(String... peerBlessings) {
        try {
            return nativeForPeer(this.nativePtr, peerBlessings);
        } catch (VException e) {
            throw new RuntimeException("Couldn't get blessings for peers", e);
        }
    }
    @Override
    public void setDefaultBlessings(Blessings blessings) throws VException {
        nativeSetDefaultBlessings(this.nativePtr, blessings);
    }
    @Override
    public Blessings defaultBlessings() {
        try {
            return nativeDefaultBlessings(this.nativePtr);
        } catch (VException e) {
            throw new RuntimeException("Couldn't get default blessings", e);
        }
    }
    @Override
    public ECPublicKey publicKey() {
        try {
            return nativePublicKey(this.nativePtr);
        } catch (VException e) {
            throw new RuntimeException("Couldn't get public key", e);
        }
    }
    @Override
    public Map<BlessingPattern, Blessings> peerBlessings() {
        try {
            return nativePeerBlessings(this.nativePtr);
        } catch (VException e) {
            throw new RuntimeException("Couldn't get peer blessings", e);
        }
    }
    @Override
    public String debugString() {
        return nativeDebugString(this.nativePtr);
    }
    @Override
    public String toString() {
        return nativeToString(this.nativePtr);
    }
    @Override
    protected void finalize() {
        nativeFinalize(this.nativePtr);
    }
}