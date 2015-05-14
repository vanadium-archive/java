// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.context;

/**
 * An extension of {@link VContext} interface that allows the user to explicitly cancel the context.
 */
public abstract class CancelableVContext implements VContext {
    /**
     * Cancels the contex.  After this method is invoked, the counter returned by
     * {@link VContext#done done} method of the new context (and all contexts further derived
     * from it) will be set to zero.
     */
    public abstract void cancel();

    /**
     * Restricts all implementations of {@link CancelableVContext} (and therefore {@link VContext})
     * to the local package.
     */
    abstract void implementationsOnlyInThisPackage();

    protected CancelableVContext() {}
}
