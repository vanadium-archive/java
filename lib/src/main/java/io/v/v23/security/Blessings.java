package io.v.v23.security;

import io.v.v23.verror.VException;

import java.security.interfaces.ECPublicKey;

/**
 * Blessings encapsulates all the cryptographic operations required to prove that a set of blessings
 * (human-readable strings) have been bound to a principal in a specific call.
 *
 * Blessings objects are meant to be presented to other principals to authenticate and authorize
 * actions.
 *
 * Blessings objects are immutable and multiple threads may invoke methods on them simultaneously.
 */
public class Blessings {
    private static native String[] nativeBlessingNames(Call call) throws VException;
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
     * (1) Satisfy all the caveats given the call.
     * (2) Be rooted in {@code call.LocalPrincipal().Roots()}.
     *
     * Caveats are considered satisfied in the given call if the {@code CaveatValidator}
     * implementation can be found in the address space of the caller and {@code validate} doesn't
     * throw an exception.
     *
     * @param  call            the call used to restrict the set of returned blessings
     * @return                 blessings satisfying the provided call
     */
    public static String[] getBlessingNames(Call call) {
        try {
            return nativeBlessingNames(call);
        } catch (VException e) {
            throw new RuntimeException("Couldn't get blessings for call", e);
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
    WireBlessings wireFormat() {
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