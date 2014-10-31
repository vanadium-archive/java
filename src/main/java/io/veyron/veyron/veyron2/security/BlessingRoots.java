package io.veyron.veyron.veyron2.security;

import io.veyron.veyron.veyron2.ipc.VeyronException;

import java.security.interfaces.ECPublicKey;

/**
 * BlessingRoots hosts the set of authoritative public keys for roots of blessings.
 */
public interface BlessingRoots {
	/**
	 * Marks {@code root} as an authoritative key for blessings that match {@code pattern}.
	 *
	 * Multiple keys can be added for the same pattern, in which case all those keys are considered
	 * authoritative for blessings that match the pattern.
	 *
	 * @param  root            root that is deemed an authoritiative key for the provided pattern.
	 * @param  pattern         pattern for which we're assigning the authoratitative key.
	 * @throws VeyronException if there was an error assigning the root.
	 */
	public void add(ECPublicKey root, BlessingPattern pattern) throws VeyronException;

	/**
	 * Returns {@code null} iff the provided root is recognized as an authority on a pattern
	 * that is matched by the blessing.
	 *
	 * @param  root            the root key we're checking for authority.
	 * @param  blessing        the blessing we're checking against the root.
	 * @throws VeyronException if the provided root is not recognized as an authority for the
	 *                         provided blessing.
	 */
	public void recognized(ECPublicKey root, String blessing) throws VeyronException;

	/**
	 * Return a human-readable string description of the roots.  This description is detailed and
	 * lists out the contents of the roots.  Use {@code toString()} method for a more succinct
	 * description.
	 *
	 * @return human-readable string description of the roots.
	 */
	public String debugString();
}
