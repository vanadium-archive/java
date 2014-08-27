package com.veyron2.security;

import com.veyron2.ipc.VeyronException;

/**
 * PublicIDStore is an interface for managing PublicIDs. All PublicIDs added to the store are
 * required to be blessing the same public key and must be tagged with a BlessingPattern.
 * By default, in IPC, a client uses a PublicID from the store to authenticate to servers identified
 * by the pattern tagged on the PublicID.
 */
public interface PublicIDStore {
	/**
	 * Adds a PublicID to the store and tags it with the provided peerPattern.
	 * The method fails if the provided PublicID has a different public key from
	 * the (common) public key of existing PublicIDs in the store. PublicIDs with
	 * multiple names are broken up into PublicIDs with at most one name and then
	 * added separately to the store.
	 *
	 * @param  id              PublicID that is added to the store.
	 * @param  peerPattern     BlessingPattern of the peer the PublicID should be associated with.
	 * @throws VeyronException if the PublicID couldn't be added to the store.
	 */
	public void add(PublicID id, BlessingPattern peerPattern) throws VeyronException;

	/**
	 * Returns a PublicID by combining all PublicIDs from the store that are tagged with patterns
	 * matching the provided peer. The combined PublicID has the same public key as the individual
	 * PublicIDs and carries the union of the set of names of the individual PublicIDs.
	 * An error is returned if there are no matching PublicIDs.
	 *
	 * @param  peer            PublicID of the peer.
	 * @return                 PublicID to be used when communicating with the provided peer.
	 * @throws VeyronException if no PublicID is matching the provided peer.
	 */
	public PublicID getPeerID(PublicID peer) throws VeyronException;

	/**
	 * Returns a PublicID from the store based on the default BlessingPattern.  The returned
	 * PublicID has the same public key as the common public key of all PublicIDs in the store and
	 * carries the union of the set of names of all PublicIDs that match the default pattern.
	 * An error is returned if there are no matching PublicIDs. (Note that it is the PublicIDs that
	 * are matched with the default pattern rather than the peer pattern tags on them.)
	 *
	 * @return                 PublicID matching the default BlessingPattern.
	 * @throws VeyronException if no PublicID matches the default BlessingPattern.
	 */
	public PublicID defaultPublicID() throws VeyronException;

	/**
	 * Sets the default BlessingPattern.  In the absence of any calls to this method, the default
	 * BlessingPattern is assumed to be "*", which matches all PublicIDs.
	 *
	 * @param  pattern         the new default BlessingPattern.
	 * @throws VeyronException if the new default principal pattern couldn't be set. 
	 */
	public void setDefaultBlessingPattern(BlessingPattern pattern) throws VeyronException;
}
