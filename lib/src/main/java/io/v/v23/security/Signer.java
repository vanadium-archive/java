package io.v.v23.security;

import io.v.v23.verror.VException;
import io.v.v23.security.Signature;

import java.security.interfaces.ECPublicKey;

/**
 * Signer is the interface for signing arbitrary length messages using ECDSA private keys.
 */
public interface Signer {
    /**
     * Signs an arbitrary length message (often the hash of a larger message) using the private
     * key associated with this signer.
     *
     * The provided purpose is appended to message before signing and is made available
     * (in cleartext) with the signature.  Thus, a non-{@code null} purpose can be used to avoid
     * "type attacks", wherein an honest entity is cheated on interpreting a field in a message
     * as one with a type other than the intended one.
     *
     * @param  message         a message to be signed.
     * @param  purpose         purpose of the message, used for preventing "type attacks".
     * @return                 the message signature.
     * @throws VException      if the message cannot be signed.
     */
    public Signature sign(byte[] purpose, byte[] message) throws VException;

    /**
     * Returns ECDSA public key corresponding to this signer's private key.
     *
     * @return  an ECDSA public key corresponding to this signer's private key.
     */
    public ECPublicKey publicKey();
}