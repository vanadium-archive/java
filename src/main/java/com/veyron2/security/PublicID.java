package com.veyron2.security;

import com.veyron2.ipc.VeyronException;

import java.security.interfaces.ECPublicKey;

/**
 * PublicID is the interface for the non-secret component of a principal's unique identity.
 */
public interface PublicID {
	/**
	 * Returns a list of human-readable names associated with the principal.
	 * The returned names act only as a hint, there is no guarantee that they are
	 * globally unique.
	 */
	public String[] names();

	/**
	 * Verifies if the principal has a name matching the provided pattern or can obtain a name
	 * matching the pattern by manipulating its PublicID using PrivateID operations
	 * (e.g., <PrivateID>.bless(<PublicID>, ..., ...)). The provided pattern may be of one of the
	 * following forms:
	 * - pattern "*" matching all principals regardless of the names they have.
	 * - a specific name <name> matching all principals who have a name that can be
	 *   extended to <name>.
	 * - pattern <name>/* matching all principals who have a name that is an extension of the
	 *   name <name>.
	 *
	 * @param  pattern that pattern used for name-matching.
	 * @return         true iff the principal has a name matching the provided pattern.
	 */
	public boolean match(PrincipalPattern pattern);

	/**
	 * Returns the public key corresponding to the private key
	 * that is held only by the principal represented by this PublicID.
	 *
	 * @return public key held by the principal.
	 */
	public ECPublicKey publicKey();

	/**
	 * Determines whether the PublicID has credentials that are valid under the provided context.
	 * If so, Authorize returns a new PublicID that carries only the valid credentials. The returned
	 * PublicID is always non-nil in the absence of errors and has the same public key as this
	 * PublicID.
	 *
	 * @param  context         the context under which credentials are evaluated.
	 * @return                 the PublicID that carries only the valid credentials.
	 * @throws VeyronException if any error is encountered during the evaluation.
	 */
	public PublicID authorize(Context context) throws VeyronException;

	/**
	 * Returns the set of third-party restrictions on the scope of the identity. The returned
	 * restrictions are wrapped in ServiceCaveats according to the services they are bound to.
	 *
	 * @return the set of third-party restrictions on the scope of the identity.
	 */
	public ServiceCaveat[] thirdPartyCaveats();
}