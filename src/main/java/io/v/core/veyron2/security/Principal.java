package io.v.core.veyron2.security;

import io.v.core.veyron2.verror.VException;

import java.security.interfaces.ECPublicKey;
import java.util.Map;

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
     * caveat to be provided. If unconstrained delegation is desired, the
     * {@code Security.newUnconstrainedUseCaveat()} method can be used to produce this argument.
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
     * @throws VException        if the blessee couldn't be blessed.
     */
    public Blessings bless(ECPublicKey key, Blessings with, String extension, Caveat caveat,
        Caveat... additionalCaveats) throws VException;

    /**
     * Creates a blessing with the provided name for this principal.
     *
     * @param  name            the name to bless self with.
     * @param  caveats         caveats on the blessings.
     * @return                 the resulting blessings.
     * @throws VException      if there was an error blessing self.
     */
    public Blessings blessSelf(String name, Caveat... caveats) throws VException;

    /**
     * Uses the private key of the principal to sign message.
     *
     * @param  message         the message to be signed.
     * @return                 signature of the message.
     * @throws VException      if the message couldn't be signed.
     */
    public Signature sign(byte[] message) throws VException;

    /**
     * Returns the public key counterpart of the private key held by the principal.
     *
     * @return the public key held by the principal.
     */
    public ECPublicKey publicKey();

    /**
     * Returns blessings granted to this principal from recognized authorities
     * (i.e., blessing roots) whose human-readable strings match a given name pattern.
     * This method does not check the validity of the caveats in the returned blessings.
     *
     * @param  name a pattern against which blessings are matched.
     * @return      blessings whose human-readable strings match a given name pattern.
     */
    public Blessings[] blessingsByName(BlessingPattern name);

    /**
     * Returns human-readable strings for the provided blessings, along with the caveats associated
     * with them.  The provided blessings must belong to this principal and must have been granted
     * to it from recognized authorities (i.e., blessing roots).
     *
     * This method does not validate caveats on the provided blessings and thus may NOT be
     * valid in certain contexts.  (Use {@code Blessings.forContext(ctx)} to determine the set of
     * valid blessing strings in a particular context.)
     *
     * @param blessings blessings whose human-readable strings are to be returned.
     * @return          human-readable strings of the provided blessings, along with the caveats
     *                  associated with them
     */
    public Map<String, Caveat[]> blessingsInfo(Blessings blessings);

    /**
     * Provides access to the BlessingStore containing blessings that have been granted to this
     * principal.
     *
     * @return BlessingStore containing blessings that have been granted to this principal.
     */
    public BlessingStore blessingStore();

    /**
     * Returns the set of recognized authorities (identified by their public keys) on blessings that
     * match specific patterns.
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
     * @throws VException      if there was an error assigning the said authorities.
     */
    public void addToRoots(Blessings blessings) throws VException;
}