// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.
package io.v.v23.syncbase;

import io.v.v23.context.VContext;
import io.v.v23.syncbase.util.AccessController;
import io.v.v23.verror.VException;

/**
 * The interface for a Vanadium Syncbase service.
 */
public interface SyncbaseService extends AccessController {
    /**
     * Returns the full (i.e., object) name of this service.
     */
    String fullName();

    /**
     * Returns the handle to an app with the given name.
     * <p>
     * Note that this app may not yet exist and can be created using the
     * {@link SyncbaseApp#create} call.
     *
     * @param  relativeName name of the given app.  May not contain slashes
     * @return              the handle to an app with the given name
     */
    SyncbaseApp getApp(String relativeName);

    /**
     * Returns a list of all relative app names.
     *
     * @param  ctx        Vanadium context
     * @throws VException if the list of app names couldn't be retrieved
     */
    String[] listApps(VContext ctx) throws VException;
}