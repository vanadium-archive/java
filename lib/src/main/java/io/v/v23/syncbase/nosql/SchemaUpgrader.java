// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.syncbase.nosql;

import io.v.v23.verror.VException;

/**
 * An interface that must be implemented by an app in order to upgrade the database schema from
 * a lower version to a higher version.
 */
public interface SchemaUpgrader {
    /**
     * Updgrades database from an old to the new schema version.
     * <p>
     * This method must be idempotent.
     *
     * @param  db         database to be upgraded
     * @param  oldVersion old schema version
     * @param  newVersion new schema version
     * @throws VException if the database couldn't be upgraded
     */
    void run(Database db, int oldVersion, int newVersion) throws VException;
}