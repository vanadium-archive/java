// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.security;

import io.v.v23.verror.VException;

public class BlessingPatternWrapper {
    private static native BlessingPatternWrapper nativeWrap(BlessingPattern pattern)
            throws VException;

    /**
     * Wraps the provided blessing pattern.
     *
     * @param  pattern         the blessing pattern being wrapped.
     * @return                 wrapped blessing pattern.
     * @throws VException      if the blessing pattern couldn't be wrapped.
     */
    public static BlessingPatternWrapper wrap(BlessingPattern pattern) throws VException {
        return nativeWrap(pattern);
    }

    private native boolean nativeIsMatchedBy(long nativePtr, String[] blessings);
    private native boolean nativeIsValid(long nativePtr);
    private native BlessingPatternWrapper nativeMakeNonExtendable(long nativePtr) throws VException;
    private native void nativeFinalize(long nativePtr);

    private long nativePtr;
    private BlessingPattern pattern;

    private BlessingPatternWrapper(long nativePtr, BlessingPattern pattern) {
        this.nativePtr = nativePtr;
        this.pattern = pattern;
    }

    /**
     * Returns {@code true} iff one of the presented blessings matches this pattern as per
     * the rules described in documentation for the {@code BlessingPattern} type.
     *
     * @param  blessings blessings compared against this pattern.
     * @return           true iff one of the presented blessings matches this pattern.
     */
    public boolean isMatchedBy(String... blessings) {
        return nativeIsMatchedBy(this.nativePtr, blessings);
    }

    /**
     * Returns {@code true} iff this pattern is well formed, i.e., does not contain any character
     * sequences that will cause the BlessingPattern to never match any valid blessings.
     *
     * @return true iff the pattern is well formed.
     */
    public boolean isValid() {
        return nativeIsValid(this.nativePtr);
    }

    /**
     * Returns a new pattern that is not matched by any extension of the blessing represented
     * by this pattern.
     *
     * For example:
     * <code>
     *     final BlessingPatternWrapper onlyAlice = BlessingPatternWrapper.create(
     *         new BlessingPattern("google/alice")).makeNonExtendable();
     *     onlyAlice.MatchedBy("google");                  // Returns true
     *     onlyAlice.MatchedBy("google/alice");            // Returns true
     *     onlyAlice.MatchedBy("google/alice/bob");        // Returns false
     * </code>
     *
     * @return a pattern that matches all extensions of the blessings that are matched by this
     *         pattern.
     */
    public BlessingPatternWrapper makeNonExtendable() {
        try {
            return nativeMakeNonExtendable(this.nativePtr);
        } catch (VException e) {
            throw new RuntimeException("Couldn't make glob", e);
        }
    }

    /*
     * Returns the blessing pattern contained in the wrapper.
     *
     * @return the blessing pattern contained in the wrapper.
     */
    public BlessingPattern getPattern() {
        return this.pattern;
    }

    @Override
    protected void finalize() {
        nativeFinalize(this.nativePtr);
    }
}