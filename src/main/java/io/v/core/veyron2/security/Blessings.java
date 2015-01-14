package io.v.core.veyron2.security;

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
	public abstract String[] forContext(VContext context);

	/**
	 * Returns the public key of the principal to which blessings in this object are bound.
	 *
	 * @return public key of the principal to whom the blessings are bound.
	 */
	public abstract ECPublicKey publicKey();

	/**
	 * Returns the blessings in the wire format.
	 *
	 * @return wire format of the blessings.
	 */
	public abstract WireBlessings wireFormat();

	/**
	 * Method that restricts all implementations of CancelableContext (and therefore Context)
	 * to the local package.
	 */
	abstract void implementationsOnlyInThisPackage();
}