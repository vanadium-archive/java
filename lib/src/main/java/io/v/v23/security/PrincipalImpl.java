// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.security;

import io.v.v23.verror.VException;

import java.security.interfaces.ECPublicKey;
import java.util.Map;

public class PrincipalImpl implements Principal {
    private final long nativePtr;
    private final Signer signer;
    private final BlessingStore store;
    private final BlessingRoots roots;

    private static native PrincipalImpl nativeCreate() throws VException;
    private static native PrincipalImpl nativeCreateForSigner(Signer signer) throws VException;
    private static native PrincipalImpl nativeCreateForAll(Signer signer, BlessingStore store,
        BlessingRoots roots) throws VException;
    private static native PrincipalImpl nativeCreatePersistent(String passphrase, String dir)
            throws VException;
    private static native PrincipalImpl nativeCreatePersistentForSigner(Signer signer, String dir)
            throws VException;

    static PrincipalImpl create() throws VException {
        return nativeCreate();
    }
    static PrincipalImpl create(Signer signer) throws VException {
        return nativeCreateForSigner(signer);
    }
    static PrincipalImpl create(Signer signer, BlessingStore store, BlessingRoots roots)
        throws VException {
        return nativeCreateForAll(signer, store, roots);
    }
    static PrincipalImpl createPersistent(String passphrase, String dir) throws VException {
        return nativeCreatePersistent(passphrase, dir);
    }
    static PrincipalImpl createPersistent(Signer signer, String dir) throws VException {
        return nativeCreatePersistentForSigner(signer, dir);
    }

    private native Blessings nativeBless(long nativePtr, ECPublicKey key, Blessings with,
        String extension, Caveat caveat, Caveat[] additionalCaveats) throws VException;
    private native Blessings nativeBlessSelf(long nativePtr, String name, Caveat[] caveats)
            throws VException;
    private native Signature nativeSign(long nativePtr, byte[] message) throws VException;
    private native ECPublicKey nativePublicKey(long nativePtr) throws VException;
    private native Blessings[] nativeBlessingsByName(long nativePtr, BlessingPattern name)
            throws VException;
    private native Map<String, Caveat[]> nativeBlessingsInfo(long nativePtr, Blessings blessings)
            throws VException;
    private native BlessingStore nativeBlessingStore(long nativePtr) throws VException;
    private native BlessingRoots nativeRoots(long nativePtr) throws VException;
    private native void nativeAddToRoots(long nativePtr, Blessings blessings)
            throws VException;
    private native void nativeFinalize(long nativePtr);

    private PrincipalImpl(long nativePtr, Signer signer, BlessingStore store, BlessingRoots roots) {
        this.nativePtr = nativePtr;
        this.signer = signer;
        this.store = store;
        this.roots = roots;
    }

    @Override
    public Blessings bless(ECPublicKey key, Blessings with, String extension, Caveat caveat,
        Caveat... additionalCaveats) throws VException {
        return nativeBless(this.nativePtr, key, with, extension, caveat, additionalCaveats);
    }
    @Override
    public Blessings blessSelf(String name, Caveat... caveats) throws VException {
        return nativeBlessSelf(this.nativePtr, name, caveats);
    }
    @Override
    public Signature sign(byte[] message) throws VException {
        if (this.signer != null) {
            final byte[] purpose = Constants.SIGNATURE_FOR_MESSAGE_SIGNING.getBytes();
            return this.signer.sign(purpose, message);
        }
        return nativeSign(this.nativePtr, message);
    }
    @Override
    public ECPublicKey publicKey() {
        if (this.signer != null) {
            return this.signer.publicKey();
        }
        try {
            return nativePublicKey(this.nativePtr);
        } catch (VException e) {
            throw new RuntimeException("Couldn't get public key", e);
        }
    }
    @Override
    public Blessings[] blessingsByName(BlessingPattern name) {
        try {
            return nativeBlessingsByName(this.nativePtr, name);
        } catch (VException e) {
            throw new RuntimeException("Couldn't get blessings for name", e);
        }
    }
    @Override
    public Map<String, Caveat[]> blessingsInfo(Blessings blessings) {
        try {
            return nativeBlessingsInfo(this.nativePtr, blessings);
        } catch (VException e) {
            throw new RuntimeException(
                    "Couldn't get human-readable strings for blessings", e);
        }
    }
    @Override
    public BlessingStore blessingStore() {
        if (this.store != null) {
            return this.store;
        }
        try {
            return nativeBlessingStore(this.nativePtr);
        } catch (VException e) {
            throw new RuntimeException("Couldn't get Blessing Store", e);
        }
    }
    @Override
    public BlessingRoots roots() {
        if (this.roots != null) {
            return this.roots;
        }
        try {
            return nativeRoots(this.nativePtr);
        } catch (VException e) {
            throw new RuntimeException("Couldn't get Blessing Store", e);
        }
    }
    @Override
    public void addToRoots(Blessings blessings) throws VException {
        nativeAddToRoots(this.nativePtr, blessings);
    }
    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null) return false;
        if (this.getClass() != other.getClass()) return false;
        return this.nativePtr == ((PrincipalImpl) other).nativePtr;
    }
    @Override
    public int hashCode() {
        return Long.valueOf(this.nativePtr).hashCode();
    }
    @Override
    public void finalize() {
        nativeFinalize(this.nativePtr);
    }
}