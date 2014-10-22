package io.veyron.veyron.veyron2.security;

import io.veyron.veyron.veyron2.ipc.VeyronException;

import java.security.interfaces.ECPublicKey;

/**
 * BlessingStore is the interface for storing blessings bound to a principal and managing the subset
 * of blessings to be presented to particular peers.
 */
public interface BlessingStore {
	/**
	 * Marks the set of blessings to be shared with peers.
	 *
	 * <code>set(b, pattern)</code> marks the intention to reveal <code>b</code> to peers who
	 * present blessings of their own matching pattern.
	 *
	 * If multiple calls to Set are made with the same pattern, the last call prevails.
	 *
	 * <code>set(nil, pattern)</code> can be used to remove the blessings previously associated with
	 * the pattern (by a prior call to <code>set</code>).
	 *
	 * It is an error to call <code>set</code> with blessings whose public key does not match the
	 * PublicKey of the principal for which this store hosts blessings.
	 *
	 * @param  blessings       Blessings to be revealed to the specified peers.
	 * @param  forPeers        a peer to whom the Blessings should be revealed.
	 * @return                 Blessings previously associated with the specified pattern.
	 * @throws VeyronException if there was an error making the association.
	 */
	public Blessings set(Blessings blessings, BlessingPattern forPeers) throws VeyronException;

	/**
	 * Returns the set of blessings that have been previously added to the store with an intent of
	 * being shared with peers that have at least one of the provided blessings.
	 *
	 * If no peerBlessings are provided then blessings marked for all peers
	 * (i.e., Add-ed with the AllPrincipals pattern) is returned.
	 *
	 * Returns nil if there are no matching blessings in the store.
	 *
	 * @param  peerBlessings [description]
	 * @return               [description]
	 */
	public Blessings forPeer(String... peerBlessings);

	/**
	 * Sets up the Blessings made available on a subsequent call to <code>default</code>.
	 *
	 * It is an error to call <code>setDefault</code> with Blesssings whose public key does not
	 * match the PublicKey of the principal for which this store hosts blessings.
	 *
	 * @param  blessings       Blessings made available on a subsequent call to <code>default</code>
	 * @throws VeyronException if there was an error setting the default blessings.
	 */
	public void setDefaultBlessings(Blessings blessings) throws VeyronException;

	/**
	 * Returns the blessings to be shared with peers for which no other information is
	 * available in order to select blessings from the store.
	 *
	 * For example, <code>default</code> can be used by servers to identify themselves to clients
	 * before the client has identified itself.
	 *
	 * <code>default</code> returns the blessings provided to the last call to setDefault, or if no
	 * such call was made it is equivalent to <code>forPeer</code> with no arguments.
	 *
	 * Returns <code>null</code> if there is no usable blessing.
	 *
	 * @return the blessings to be shared with peers for which no other information is available.
	 */
	public Blessings defaultBlessings();

	/**
	 * PublicKey returns the public key of the Principal for which this store hosts blessings.
	 *
	 * @return public key of the Principal for which this store hosts blessings.
	 */
	public ECPublicKey publicKey();

	/**
	 * Return a human-readable string description of the store.  This description is detailed and
	 * lists out the contents of the store.  Use <code>toString()</code> for a more succinct
	 * description.
	 *
	 * @return human-readable string description of the store.
	 */
	public String debugString();
}