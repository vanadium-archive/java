package com.veyron2.security;

import com.veyron2.ipc.VeyronException;
import com.veyron2.security.Signature;

import java.security.interfaces.ECPublicKey;

/**
 * Signer is the interface for signing arbitrary length messages using ECDSA private keys.
 */
public interface Signer {
	/**
	 * Signs an arbitrary length message (often the hash of a larger message) using the private
	 * key associated with this signer.
	 *
	 * @param  message         a message to be signed.
	 * @return                 the message signature.
	 * @throws VeyronException if the message cannot be signed.
	 */
	public Signature sign(byte[] message) throws VeyronException;

	/**
	 * Returns ECDSA public key corresponding to this Signer's private key.
	 *
	 * @return  an ECDSA public key corresponding to this Signer's private key.
	 */
	public ECPublicKey publicKey();
}