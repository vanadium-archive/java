// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.syncbase;

/**
 * Various syncbase utility methods.
 */
public class Syncbase {
    /**
     * Returns a new client handle to a syncbase service running at the given name.
     *
     * @param  fullName full (i.e., object) name of the syncbase service
     */
    public static SyncbaseService newService(String fullName) {
        return new SyncbaseServiceImpl(fullName);
    }

    private Syncbase() {}
}
