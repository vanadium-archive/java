// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase;

import java.util.Iterator;

public class Database implements DatabaseHandle {
    public Id getId() {
        return null;
    }

    public Collection collection(String name, CollectionOptions opts) {
        return null;
    }

    public Collection getCollection(Id id) {
        return null;
    }

    public Iterator<Collection> getCollections() {
        return null;
    }

    class SyncgroupOptions {
        // TODO(sadovsky): Fill this in.
    }

    // FOR ADVANCED USERS. Creates syncgroup and adds it to the user's "userdata" collection, as
    // needed. Idempotent.
    public Syncgroup syncgroup(String name, Collection[] collections, SyncgroupOptions opts) {
        return null;
    }

    public Syncgroup getSyncgroup(Id id) {
        return null;
    }

    public Iterator<Syncgroup> getSyncgroups() {
        return null;
    }

    public class AddSyncgroupInviteHandlerOptions {
        // TODO(sadovsky): Fill this in.
    }

    // Notifies 'h' of any existing syncgroup invites, and of all subsequent new invites.
    public void addSyncgroupInviteHandler(SyncgroupInviteHandler h, AddSyncgroupInviteHandlerOptions opts) {

    }

    public void removeSyncgroupInviteHandler(SyncgroupInviteHandler h) {

    }

    public void removeSyncgroupInviteHandlers() {

    }

    // Joins syncgroup and adds it to the user's "userdata" collection, as needed.
    // TODO(sadovsky): Should we call this "joinSyncgroup"? Also, should we add "accept" and
    // "ignore" methods to the SyncgroupInvite class, or should we treat it as a POJO?
    // TODO(sadovsky): Make this method async.
    public Syncgroup acceptSyncgroupInvite(SyncgroupInvite invite) {
        return null;
    }

    // Records that the user has ignored this invite, such that it's never surfaced again.
    // Note: This will be one of the last things we implement.
    public void ignoreSyncgroupInvite(SyncgroupInvite invite) {

    }

    public BatchDatabase beginBatch() {
        return null;
    }

    public class AddWatchChangeHandlerOptions {
        // TODO(sadovsky): Fill this in.
    }

    // Notifies 'h' of initial state, and of all subsequent changes to this database. Options struct
    // includes resume marker.
    // Note: Eventually we'll add a watch variant that takes a query, where the query can be
    // constructed using some sort of query builder API.
    public void addWatchChangeHandler(WatchChangeHandler h, AddWatchChangeHandlerOptions opts) {

    }

    public void removeWatchChangeHandler(WatchChangeHandler h) {

    }

    public void removeAllWatchChangeHandlers() {

    }
}
