package io.v.core.veyron2.security;

import io.v.core.veyron2.verror.VException;

import java.security.interfaces.ECPublicKey;
import java.util.Map;

/**
 * BlessingStore is the interface for storing blessings bound to a principal and managing the subset
 * of blessings to be presented to particular peers.
 */
public interface BlessingStore {
    /**
     * Marks the set of blessings to be shared with peers.
     *
     * {@code set(b, pattern)} marks the intention to reveal {@code b} to peers who
     * present blessings of their own matching pattern.
     *
     * If multiple calls to {@code set} are made with the same pattern, the last call prevails.
     *
     * {@code set(nil, pattern)} can be used to remove the blessings previously associated with
     * the pattern (by a prior call to {@code set}).
     *
     * It is an error to call {@code set} with blessings whose public key does not match the
     * PublicKey of the principal for which this store hosts blessings.
     *
     * @param  blessings       blessings to be revealed to the specified peers.
     * @param  forPeers        a peer to whom the blessings should be revealed.
     * @return                 blessings previously associated with the specified pattern.
     * @throws VException      if there was an error making the association.
     */
    public Blessings set(Blessings blessings, BlessingPattern forPeers) throws VException;

    /**
     * Returns the set of blessings that have been previously added to the store with an intent of
     * being shared with peers that have at least one of the provided blessings.
     *
     * If no peer blessings are provided then blessings marked for all peers (i.e., those added
     * with the {@code AllPrincipals} pattern) is returned.
     *
     * Returns {@code null} if there are no matching blessings in the store.
     *
     * @param  peerBlessings human-readable peer blessings we're retrieving blessings for.
     * @return               the set of blessings that have been previously added to the store with
     *                       an intent of being shared with peers that have at least one of the
     *                       provided (human-readable) blessings.
     */
    public Blessings forPeer(String... peerBlessings);

    /**
     * Sets up the blessings made available on a subsequent call to {@code defaultBlessings()}.
     *
     * It is an error to call {@code setDefaultBlessings()} with blessings whose public key does
     * not match the public key of the principal for which this store hosts blessings.
     *
     * @param  blessings       blessings made available on a subsequent call to
     *                         {@code defaultBlessings()}.
     * @throws VException      if there was an error setting the default blessings.
     */
    public void setDefaultBlessings(Blessings blessings) throws VException;

    /**
     * Returns the blessings to be shared with peers for which no other information is
     * available in order to select blessings from the store.
     *
     * For example, {@code defaultBlessings()} can be used by servers to identify themselves to
     * clients before the client has identified itself.
     *
     * {@code defaultBlessings()} returns the blessings provided to the last call to
     * {@code setDefaultBlessings()}, or if no such call was made it is equivalent to
     * {@code forPeer()} with no arguments.
     *
     * Returns {@code null} if there is no usable blessing.
     *
     * @return blessings to be shared with peers for which no other information is available.
     */
    public Blessings defaultBlessings();

    /**
     * Returns the public key of the principal for which this store hosts blessings.
     *
     * @return public key of the principal for which this store hosts blessings.
     */
    public ECPublicKey publicKey();

    /**
     * Returns all the blessings that the store currently holds for various peers.
     *
     * @return all the blessings that the store currently holds for various peers
     */
    public Map<BlessingPattern, Blessings> peerBlessings();

    /**
     * Return a human-readable string description of the store.  This description is detailed and
     * lists out the contents of the store.  Use {@code toString()} for a more succinct
     * description.
     *
     * @return human-readable string description of the store.
     */
    public String debugString();
}