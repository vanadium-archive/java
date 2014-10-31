package io.veyron.veyron.veyron2.security;

import java.security.interfaces.ECPublicKey;

/**
 * Blessings encapsulates all the cryptographic operations required to prove that a set of blessings
 * (human-readable strings) have been bound to a principal in a specific context.
 *
 * Blessings objects are meant to be presented to other principals to authenticate and authorize
 * actions.
 */
public abstract class Blessings {
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
	public abstract String[] forContext(Context context);

	/**
	 * Returns the public key of the principal to which blessings in this object are bound.
	 *
	 * @return public key of the principal to whom the blessings are bound.
	 */
	public abstract ECPublicKey publicKey();

	/**
	 * Returns the certificate chains corresponding to the blessings stored in this object.
	 *
	 * This method is protected in order to restrict the implementations of this class to the
	 * current package.
	 *
	 * @return the certificate chains corresponding to the blessings stored in this object.
	 */
	abstract Certificate[][] certificateChains();
}