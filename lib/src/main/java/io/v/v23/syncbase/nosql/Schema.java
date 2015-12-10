// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.syncbase.nosql;

import io.v.v23.services.syncbase.nosql.SchemaMetadata;

/**
 * A database schema.
 * <p>
 * Each database has a schema associated with it which defines the current version of the
 * database.  When a new version of an app wishes to change its data in a way that it is not
 * compatible with the old app's data, this app must change the schema version and provide relevant
 * upgrade logic in the specified {@link SchemaUpgrader}. The conflict resolution rules are also
 * associated with the schema version. Hence if the conflict resolution rules change then the schema
 * version also must be bumped.
 */
public class Schema {
    private final SchemaMetadata metadata;
    private final SchemaUpgrader upgrader;
    private final ConflictResolver resolver;

    /**
     * Creates a new database schema with the specified metadata and schema upgrader.
     * <p>
     * Note: {@link SchemaUpgrader} is purely local and is not persisted.
     */
    public Schema(SchemaMetadata metadata, SchemaUpgrader upgrader, ConflictResolver resolver) {
        this.metadata = metadata;
        this.upgrader = upgrader;
        this.resolver = resolver;
    }

    /**
     * Returns the metadata related to this schema.
     */
    public SchemaMetadata getMetadata() { return this.metadata; }

    /**
     * Returns the upgrade logic used for upgrading the schema when an app's schema version differs
     * from the database's schema version.
     */
    public SchemaUpgrader getUpgrader() { return this.upgrader; }

    /**
     * Returns a resolver that is used for conflict resolution.
     */
    public ConflictResolver getResolver() { return this.resolver; }
}