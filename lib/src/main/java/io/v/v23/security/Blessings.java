// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.security;

import io.v.v23.verror.VException;

import java.io.Serializable;
import java.security.interfaces.ECPublicKey;
import java.util.List;

/**
 * Encapsulator of all the cryptographic operations required to prove that a set of blessings
 * (human-readable strings) have been bound to a principal in a specific call.
 * <p>
 * {@link Blessings} objects are meant to be presented to other principals to authenticate
 * and authorize actions.  Functions {@link VSecurity#getLocalBlessingNames} and
 * {@link VSecurity#getRemoteBlessingNames} can be used to uncover the blessing names encapsulated
 * in these objects.
 * <p>
 * {@link Blessings} objects are immutable and multiple threads may invoke methods on
 * them simultaneously.
 * <p>
 * See also: <a href="https://v.io/glossary.html#blessing">https://v.io/glossary.html#blessing</a>.
 */
public class Blessings implements Serializable {
    private static final long serialVersionUID = 1L;

    private static native Blessings nativeCreate(WireBlessings wire) throws VException;
    private static native Blessings nativeCreateUnion(Blessings[] blessings) throws VException;

    public static Blessings create(WireBlessings wire) {
        try {
            return nativeCreate(wire);
        } catch (VException e) {
            throw new RuntimeException(e);
        }
    }

    static Blessings createUnion(Blessings... blessings) throws VException {
        return nativeCreateUnion(blessings);
    }

    private final long nativePtr;
    private final WireBlessings wire;  // non-null

    private native ECPublicKey nativePublicKey(long nativePtr) throws VException;
    private native void nativeFinalize(long nativePtr);

    /**
     * Creates new {@link Blessings} from the provided certificate chains.
     *
     * @param  chains certificate chains constituting these blessings
     */
    public Blessings(List<List<VCertificate>> chains) {
        this(Blessings.create(new WireBlessings(chains)));
    }

    /**
     * Creates new empty {@link Blessings}.
     */
    public Blessings() {
        this(Blessings.create(new WireBlessings()));
    }

    private Blessings(long nativePtr, WireBlessings wire) {
        this.nativePtr = nativePtr;
        this.wire = wire;
    }

    private Blessings(Blessings other) {
        this.nativePtr = other.nativePtr;
        this.wire = other.wire;
    }

    /**
     * Returns the public key of the principal to which blessings in this object are bound.
     * The return value may be {@code null} if the blessings are empty.
     */
    public ECPublicKey publicKey() {
        try {
            return nativePublicKey(this.nativePtr);
        } catch (VException e) {
            throw new RuntimeException("Couldn't get public key", e);
        }
    }

    /**
     * Returns the blessings in the wire format.
     */
    public WireBlessings wireFormat() {
        return this.wire;
    }

    /**
     * Returns {@code true} iff the blessings are empty.
     */
    public boolean isEmpty() {
        return this.wire.getCertificateChains().isEmpty();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (!(obj instanceof Blessings)) return false;
        Blessings other = (Blessings) obj;
        return this.wireFormat().equals(other.wireFormat());
    }
    @Override
    public int hashCode() {
        return this.wire.hashCode();
    }
    @Override
    public String toString() {
        return this.wire.toString();
    }
    @Override
    protected void finalize() {
        nativeFinalize(this.nativePtr);
    }

    public List<List<VCertificate>> getCertificateChains() {
        return wire.getCertificateChains();
    }
}
