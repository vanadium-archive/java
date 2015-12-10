// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.
package io.v.v23.syncbase.nosql;

import io.v.v23.context.VContext;

/**
 * An interface for conflict resolution.
 * <p>
 * The intention is that the apps would provide their own conflict resolution by implementing this
 * interface and attaching it to a database {@link Schema}.
 */
public interface ConflictResolver {
    /**
     * Returns a resolution for the provided {@code conflict}.
     */
    Resolution onConflict(VContext ctx, Conflict conflict);
}
