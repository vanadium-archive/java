// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.rx.syncbase;

import io.v.v23.context.VContext;
import io.v.v23.security.access.Permissions;
import io.v.v23.verror.ExistException;
import io.v.v23.verror.VException;

abstract class SyncbaseEntity implements ExistenceAware, Creatable {
    public static SyncbaseEntity compose(final ExistenceAware fnExists, final Creatable fnCreate) {
        return new SyncbaseEntity() {
            @Override
            public void create(VContext vContext, Permissions permissions) throws VException {
                fnCreate.create(vContext, permissions);
            }

            @Override
            public boolean exists(VContext vContext) throws VException {
                return fnExists.exists(vContext);
            }
        };
    }

    /**
     * Utility for Syncbase entities with lazy creation semantics. It would be great if this were
     * instead factored into a V23 interface and utility.
     */
    public void ensureExists(final VContext vContext, final Permissions permissions)
            throws VException {
        if (!exists(vContext)) {
            try {
                create(vContext, permissions);
            } catch (final ExistException ignore) {
            }
        }
    }
}
