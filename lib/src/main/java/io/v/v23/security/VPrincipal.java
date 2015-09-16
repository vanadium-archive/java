// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.security;

import io.v.v23.verror.VException;

import java.security.interfaces.ECPublicKey;
import java.util.Map;

/**
 * An entity capable of making or receiving RPCs.  {@link VPrincipal}s have a unique
 * (public, private) key pair, have blessings bound to them, and can bless other principals.
 * <p>
 * Multiple goroutines may invoke methods on a {@link VPrincipal} simultaneously.
 * <p>
 * See also: <a href="https://github.com/vanadium/docs/blob/master/glossary.md#principal">https://github.com/vanadium/docs/blob/master/glossary.md#principal</a>.
 */
public interface VPrincipal {
    /**
     * Binds extensions of blessings held by this principal to another principal (represented by
     * its public key).
     * <p>
     * For example, a principal with the blessings {@code "google/alice"} and
     * {@code "vanadium/alice"} can bind the blessings {@code "google/alice/friend"} and
     * {@code "vanadium/alice/friend"} to another principal using:
     * <p><blockquote><pre>
     *   {@literal bless(<other principal>, <google/alice, vanadium/alice>, "friend", ...)}
     * </pre></blockquote><p>
     * To discourage unconstrained delegation of authority, the interface requires at least one
     * caveat to be provided. If unconstrained delegation is desired, the
     * {@link VSecurity#newUnconstrainedUseCaveat} method can be used to produce this argument.
     * <p>
     * {@code with.publicKey()} must be the same as the principal's public key.
     *
     * @param  key               public key representing the principal being blessed
     * @param  with              blessings of the current principal (i.e., the one doing the
     *                           blessing) that should be used for the blessing
     * @param  extension         extension that the blessee should be blessed with
     * @param  caveat            caveat on the blessing
     * @param  additionalCaveats addional caveats on the blessing
     * @return                   the resulting blessings
     * @throws VException        if the blessee couldn't be blessed
     */
    Blessings bless(ECPublicKey key, Blessings with, String extension, Caveat caveat,
        Caveat... additionalCaveats) throws VException;

    /**
     * Creates a blessing with the provided name for this principal.
     *
     * @param  name            the name to bless self with
     * @param  caveats         caveats on the blessings
     * @return                 the resulting blessings
     * @throws VException      if there was an error blessing self
     */
    Blessings blessSelf(String name, Caveat... caveats) throws VException;

    /**
     * Uses the private key of the principal to sign a message.
     *
     * @param  message         the message to be signed
     * @return                 signature of the message
     * @throws VException      if the message couldn't be signed
     */
    VSignature sign(byte[] message) throws VException;

    /**
     * Returns the public key counterpart of the private key held by the principal.
     */
    ECPublicKey publicKey();

    /**
     * Returns blessings granted to this principal from recognized authorities
     * (i.e., blessing roots) whose human-readable strings match a given name pattern.
     * This method does not check the validity of the caveats in the returned blessings.
     *
     * @param  name a pattern against which blessings are matched
     * @return      blessings whose human-readable strings match a given name pattern
     */
    Blessings[] blessingsByName(BlessingPattern name);

    /**
     * Returns human-readable strings for the provided blessings, along with the caveats associated
     * with them.  The provided blessings must belong to this principal and must have been granted
     * to it from recognized authorities (i.e., blessing roots).
     * <p>
     * This method does not validate caveats on the provided blessings and thus may
     * <strong>not</strong> be valid in certain calls.  (Use
     * {@link VSecurity#getRemoteBlessingNames} to determine the set of valid blessing strings in a
     * particular call.)
     *
     * @param blessings blessings whose human-readable strings are to be returned
     * @return          human-readable strings of the provided blessings, along with the caveats
     *                  associated with them
     */
    Map<String, Caveat[]> blessingsInfo(Blessings blessings);

    /**
     * Provides access to the {@link BlessingStore} containing blessings that have been granted to
     * this principal.  The returned {@link BlessingStore} is never {@code null}.
     *
     * @return a {@link BlessingStore} containing blessings that have been granted to this principal
     */
    BlessingStore blessingStore();

    /**
     * Returns the set of recognized authorities (identified by their public keys) on blessings that
     * match specific patterns.  Never returns {@code null}.
     */
    BlessingRoots roots();

    /**
     * Marks the root principals of all blessing chains represented by {@code blessings} as an
     * authority on blessing chains beginning at that root.
     * <p>
     * For example, if {@code blessings} represents the blessing chains
     * {@code ["alice/friend/spouse", "charlie/family/daughter"]} then {@code addToRoots(blessing)}
     * will mark the root public key of the chain {@code "alice/friend/bob"} as the as authority on
     * all blessings that match the pattern {@code "alice/..."}, and root public key of the chain
     * {@code "charlie/family/daughter"} as an authority on all blessings that match the pattern
     * {@code "charlie/..."}.
     *
     * @param  blessings       blessings to be used as authorities on blessing chains beginning at
     *                         those roots
     * @throws VException      if there was an error assigning the said authorities
     */
    void addToRoots(Blessings blessings) throws VException;
}
