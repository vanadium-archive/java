package io.veyron.veyron.veyron2.security;

import io.veyron.veyron.veyron2.ipc.VeyronException;

import org.joda.time.Duration;

import java.security.interfaces.ECPublicKey;

/**
 * PrivateID is the interface for the secret component of a principal's unique
 * identity.
 *
 * Each principal has a unique (private, public) key pair. The private key
 * is known only to the principal and is not expected to be shared.
 */
public interface PrivateID {
	/**
	 * Returns the non-secret component of principal's identity (which can be encoded and
	 * transmitted across the network perhaps).
	 *
	 * @return the non-secret component of principal's identity.
	 */
	public PublicID publicID();

	/**
	 * Creates a constrained PublicID from the provided one. The returned PublicID:
	 * - Has the same PublicKey as the provided one.
	 * - Has a new name which is an extension of PrivateID's name with the
	 *   provided blessingName string.
	 * - Is valid for the provided duration only if both the constraints on the
	 *   PrivateID and the provided service caveats are met.
	 *
	 * Bless assumes that the blessee is in possession of the private key corresponding
	 * to the blessee.PublicKey. Failure to ensure this property may result in
	 * impersonation attacks.
	 *
	 * @param  blessee         the public component of the identity that we are constraining.
	 * @param  blessingName    name that is appended to PrivateID's name to create a blessing name.
	 * @param  duration        the duration of validity of the blessed PublicID.
	 * @return PublicID        constrained PublicID.
	 * @throws VeyronException if any error is encountered during the blessing.
	 */
	public PublicID bless(PublicID blessee, String blessingName, Duration duration) throws VeyronException;

	/**
	 * Returns a new PrivateID that has the same secret component as a existing PrivateID but with
	 * the provided public component (PublicID).  The provided PublicID must have the same public
	 * key as the existing PublicID for this operation to succeed.
	 *
	 * @param  publicID        public component of the returned PrivateID
	 * @return                 derived PrivateID
	 * @throws VeyronException if any error is encountered
	 */
	public PrivateID derive(PublicID publicID) throws VeyronException;

	/**
	 * Signs an arbitrary length message (often the hash of a larger message) using the private
	 * key associated with this PrivateID.
	 *
	 * @param  message         a message to be signed.
	 * @return                 the message signature.
	 * @throws VeyronException if the message cannot be signed.
	 */
	public Signature sign(byte[] message) throws VeyronException;

	/**
	 * Returns ECDSA public key corresponding to this PrivateID's private key.
	 *
	 * @return  an ECDSA public key corresponding to this PrivateID's private key.
	 */
	public ECPublicKey publicKey();
}