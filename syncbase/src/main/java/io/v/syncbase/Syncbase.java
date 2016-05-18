// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase;

public class Syncbase {
    public class DatabaseOptions {
        // TODO(sadovsky): Fill this in.
    }

    // Starts Syncbase if needed; creates default database if needed; reads config (e.g. cloud
    // syncbase name) from options struct; performs create-or-join for "userdata" syncgroup if
    // needed; returns database handle. Async, and can fail.
    // TODO(sadovsky): The create-or-join will force this method to be async, which is annoying
    // since create-or-join will no longer be necessary once syncgroup merge is supported. Maybe
    // move create-or-join to a separate, temporarily-necessary init function?
    // TODO(sadovsky): Make this method async.
    public static Database database(DatabaseOptions opts) {
        return null;
    }
}