package io.v.v23.security;

import io.v.v23.verror.VException;

import java.security.interfaces.ECPublicKey;

/**
 * Blessings encapsulates all the cryptographic operations required to prove that a set of blessings
 * (human-readable strings) have been bound to a principal in a specific context.
 *
 * Blessings objects are meant to be presented to other principals to authenticate and authorize
 * actions.
 *
 * Blessings objects are immutable and multiple threads may invoke methods on them simultaneously.
 */
public class Blessings {
    private static native Blessings nativeCreate(WireBlessings wire) throws VException;
    private static native Blessings nativeCreateUnion(Blessings[] blessings) throws VException;

    static Blessings create(WireBlessings wire) throws VException {
        if (wire == null) {
            wire = new WireBlessings();
        }
        return nativeCreate(wire);
    }

    static Blessings createUnion(Blessings... blessings) throws VException {
        return nativeCreateUnion(blessings);
    }

    private final long nativePtr;
    private final WireBlessings wire;  // non-null

    private native String[] nativeForContext(long nativePtr, VContext context) throws VException;
    private native ECPublicKey nativePublicKey(long nativePtr) throws VException;
    private native void nativeFinalize(long nativePtr);

    private Blessings(long nativePtr, WireBlessings wire) {
        this.nativePtr = nativePtr;
        this.wire = wire;
    }

    /**
     * Returns a validated set of (human-readable string) blessings presented by the principal.
     * These returned blessings (strings) are guaranteed to:
     *
     * (1) Satisfy all the caveats in the given context.
     * (2) Be rooted in {@code context.LocalPrincipal().Roots()}.
     *
     * Caveats are considered satisfied in the given context if the {@code CaveatValidator}
     * implementation can be found in the address space of the caller and {@code validate} returns
     * {@code null}.
     *
     * @param  context         the security context used to restrict the set of returned blessings.
     * @return                 blessings satisfying the provided security context.
     */
    public String[] forContext(VContext context) {
        try {
            return nativeForContext(this.nativePtr, context);
        } catch (VException e) {
            throw new RuntimeException("Couldn't get blessings for context: " + e.getMessage());
        }
    }

    /**
     * Returns the public key of the principal to which blessings in this object are bound.
     * The return value may be {@code null} if the blessings are empty.
     *
     * @return public key of the principal to whom the blessings are bound or {@code null}
     *         if the blessings are empty
     */
    public ECPublicKey publicKey() {
        try {
            return nativePublicKey(this.nativePtr);
        } catch (VException e) {
            throw new RuntimeException("Coudln't get public key: " + e.getMessage());
        }
    }

    /**
     * Returns the blessings in the wire format.
     *
     * @return wire format of the blessings.
     */
    public WireBlessings wireFormat() {
        return this.wire;
    }

    /**
     * Returns true iff the blessings are empty.
     *
     * @return true iff the blessings are empty
     */
    public boolean isEmpty() {
        return this.wire.getCertificateChains().isEmpty();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (!(obj instanceof Blessings)) return false;
        final Blessings other = (Blessings) obj;
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
}