package io.veyron.veyron.veyron2.security;

import io.veyron.veyron.veyron2.ipc.VeyronException;

import java.security.interfaces.ECPublicKey;

/**
 * Principal represents an entity capable of making or receiving RPCs.  Principals have a unique
 * (public, private) key pair, have blessings bound to them, and can bless other principals.
 */
public interface Principal {
	/**
	 * Binds extensions of blessings held by this principal to another principal (represented by
	 * its public key).
	 *
	 * For example, a principal with the blessings "google/alice" and "veyron/alice" can bind the
	 * blessings "google/alice/friend" and "veyron/alice/friend" to another principal using:
	 *   {@code Bless(<other principal>, <google/alice, veyron/alice>, "friend", ...)}
	 *
	 * To discourage unconstrained delegation of authority, the interface requires at least one
	 * caveat to be provided. If unconstrained delegation is desired, the {@code UnconstrainedUse}
	 * function can be used to produce this argument.
	 *
	 * {@code with.publicKey()} must be the same as the principal's public key.
	 *
	 * @param  key               public key representing the principal being blessed.
	 * @param  with              blessings of the current principal (i.e., the one doing the
	 *                           blessing) that should be used for the blessing.
	 * @param  extension         extension that the blessee should be blessed with.
	 * @param  caveat            caveat on the blessing.
	 * @param  additionalCaveats addional caveats on the blessing.
	 * @return                   the resulting blessings.
	 * @throws VeyronException   if the blessee couldn't be blessed.
	 */
	public Blessings bless(ECPublicKey key, Blessings with, String extension, Caveat caveat,
		Caveat... additionalCaveats) throws VeyronException;

	/**
	 * Creates a blessing with the provided name for this principal.
	 *
	 * @param  name            the name to bless self with.
	 * @param  caveats         caveats on the blessings.
	 * @return                 the resulting blessings.
	 * @throws VeyronException if there was an error blessing self.
	 */
	public Blessings blessSelf(String name, Caveat... caveats) throws VeyronException;

	/**
	 * Uses the private key of the principal to sign message.
	 *
	 * @param  message         the message to be signed.
	 * @return                 signature of the message.
	 * @throws VeyronException if the message couldn't be signed.
	 */
	public Signature sign(byte[] message) throws VeyronException;

	/**
	 * Returns the public key counterpart of the private key held by the principal.
	 *
	 * @return the public key held by the principal.
	 */
	public ECPublicKey publicKey();

	/**
	 * Provides access to the BlessingStore containing blessings that have been granted to this
	 * principal.
	 *
	 * @return BlessingStore containing blessings that have been granted to this principal.
	 */
	public BlessingStore blessingStore();

	/**
	 * Returns the set of recognized authorities (identified by their public keys) on blessings that
	 * match specific patterns
	 *
	 * @return set of recognized authorities on blessings that match specific patterns.
	 */
	public BlessingRoots roots();

	/**
	 * Marks the root principals of all blessing chains represented by {@code blessings} as an
	 * authority on blessing chains beginning at that root.
	 *
	 * For example, if {@code blessings} represents the blessing chains ["alice/friend/spouse",
	 * "charlie/family/daughter"] then {@code addToRoots(blessing)} will mark the root public
	 * key of the chain "alice/friend/bob" as the as authority on all blessings that match the
	 * pattern "alice/...", and root public key of the chain "charlie/family/daughter" as an
	 * authority on all blessings that match the pattern "charlie/...".
	 *
	 * @param  blessings       blessings to be used as authorities on blessing chains beginning at
	 *                         those roots.
	 * @throws VeyronException if there was an error assigning the said authorities.
	 */
	public void addToRoots(Blessings blessings) throws VeyronException;
}