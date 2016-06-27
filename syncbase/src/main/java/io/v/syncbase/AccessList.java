// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.v.syncbase.core.Permissions;

/**
 * Specifies access levels for a set of users. Each user has an associated access level: read-only,
 * read-write, or read-write-admin.
 */
public class AccessList {
    public enum AccessLevel {
        READ,
        READ_WRITE,
        READ_WRITE_ADMIN
    }

    private Map<String, AccessLevel> users;

    /**
     * @throws IllegalArgumentException if accessList is not valid
     */
    private static Set<String> parsedAccessListToUserIds(Map<String, Set<String>> accessList) {
        Set<String> res = new HashSet<>();
        if (accessList.containsKey(Permissions.NOT_IN) &&
                !accessList.get(Permissions.NOT_IN).isEmpty()) {
            throw new IllegalArgumentException("Non-empty not-in section: " + accessList);
        }
        for (String blessingPattern : accessList.get(Permissions.IN)) {
            // TODO(alexfandrianto): What if the blessing pattern is actually "..."?
            // TODO(sadovsky): Ignore cloud peer's blessing pattern?
            res.add(Syncbase.getAliasFromBlessingPattern(blessingPattern));
        }
        return res;
    }

    /**
     * Creates an empty access list.
     */
    public AccessList() {
        this.users = new HashMap<>();
    }

    public AccessLevel getAccessLevelForUser(User user) {
        return users.get(user.getAlias());
    }

    public AccessLevel setAccessLevel(User user, AccessLevel newLevel) {
        if (newLevel == null) {
            return removeAccessLevel(user);
        }
        AccessLevel oldLevel = getAccessLevelForUser(user);
        users.put(user.getAlias(), newLevel);
        return oldLevel;
    }

    public AccessLevel removeAccessLevel(User user) {
        return users.remove(user);
    }

    /**
     * TODO(alexfandrianto): Vary implementation if this constructor needs to be called with non-
     * collection permissions. The current simplification allows us to know that read/write/admin
     * are available for parsing.
     * @throws IllegalArgumentException if corePermissions are not valid
     */
    AccessList(Permissions corePermissions) {
        Map<String, Map<String, Set<String>>> parsedPermissions = corePermissions.parse();
        Set<String> readers = parsedAccessListToUserIds(parsedPermissions.get(Permissions.Tags.READ));
        Set<String> writers = parsedAccessListToUserIds(parsedPermissions.get(Permissions.Tags.WRITE));
        Set<String> admins = parsedAccessListToUserIds(parsedPermissions.get(Permissions.Tags.ADMIN));

        // Sanity checks. Readers must contain writers, which must contain admins.
        if (!readers.containsAll(writers)) {
            throw new IllegalArgumentException("Some writers are not readers: " + writers + ", " + readers);
        }
        if (!writers.containsAll(admins)) {
            throw new IllegalArgumentException("Some admins are not writers: " + admins + ", " + writers);
        }

        // Compute the access level of each user.
        // Note: This is only correct for collection permissions.
        this.users = new HashMap<>();
        for (String userId : readers) {
            users.put(userId, AccessLevel.READ);
        }
        for (String userId : writers) {
            users.put(userId, AccessLevel.READ_WRITE);
        }
        for (String userId : admins) {
            users.put(userId, AccessLevel.READ_WRITE_ADMIN);
        }
    }

    private static void addToVAccessList(Map<String, Set<String>> accessList, String blessing) {
        if (!accessList.get(Permissions.IN).contains(blessing)) {
            accessList.get(Permissions.IN).add(blessing);
        }
    }

    private static void removeFromVAccessList(Map<String, Set<String>> accessList, String blessing) {
        accessList.get(Permissions.IN).remove(blessing);
    }


    static Permissions applyDeltaForCollection(Permissions corePermissions,
                                                         AccessList delta) {

        Map<String, Map<String, Set<String>>> parsedPermissions =
                applyDeltaParsed(corePermissions, delta);
        Map<String, Map<String, Set<String>>> filteredPermissions = new HashMap<>();
        filteredPermissions.put(Permissions.Tags.READ,
                parsedPermissions.get(Permissions.Tags.READ));
        filteredPermissions.put(Permissions.Tags.WRITE,
                parsedPermissions.get(Permissions.Tags.WRITE));
        filteredPermissions.put(Permissions.Tags.ADMIN,
                parsedPermissions.get(Permissions.Tags.ADMIN));
        return new Permissions(filteredPermissions);
    }

    /**
     * Computes a new Permissions object based on delta, allowing only the valid tags.
     */
    static Permissions applyDeltaForSyncgroup(Permissions corePermissions,
                                                        AccessList delta) {

        Map<String, Map<String, Set<String>>> parsedPermissions =
                applyDeltaParsed(corePermissions, delta);
        Map<String, Map<String, Set<String>>> filteredPermissions = new HashMap<>();
        filteredPermissions.put(Permissions.Tags.READ,
                parsedPermissions.get(Permissions.Tags.READ));
        filteredPermissions.put(Permissions.Tags.ADMIN,
                parsedPermissions.get(Permissions.Tags.ADMIN));
        return new Permissions(filteredPermissions);
    }

    /**
     * Computes a new parsed permissions map from the original Permissions object based on delta.
     */
    private static Map<String, Map<String, Set<String>>> applyDeltaParsed(
            Permissions corePermissions, AccessList delta){

        Map<String, Map<String, Set<String>>> parsedPermissions = corePermissions.parse();
        for (String userId : delta.users.keySet()) {
            AccessLevel level = delta.users.get(userId);
            String blessing = Syncbase.getBlessingStringFromAlias(userId);
            if (level == null) {
                removeFromVAccessList(parsedPermissions.get(Permissions.Tags.RESOLVE), blessing);
                removeFromVAccessList(parsedPermissions.get(Permissions.Tags.READ), blessing);
                removeFromVAccessList(parsedPermissions.get(Permissions.Tags.WRITE), blessing);
                removeFromVAccessList(parsedPermissions.get(Permissions.Tags.ADMIN), blessing);
                continue;
            }
            switch (level) {
                case READ:
                    addToVAccessList(parsedPermissions.get(Permissions.Tags.RESOLVE), blessing);
                    addToVAccessList(parsedPermissions.get(Permissions.Tags.READ), blessing);
                    removeFromVAccessList(parsedPermissions.get(Permissions.Tags.WRITE), blessing);
                    removeFromVAccessList(parsedPermissions.get(Permissions.Tags.ADMIN), blessing);
                    break;
                case READ_WRITE:
                    addToVAccessList(parsedPermissions.get(Permissions.Tags.RESOLVE), blessing);
                    addToVAccessList(parsedPermissions.get(Permissions.Tags.READ), blessing);
                    addToVAccessList(parsedPermissions.get(Permissions.Tags.WRITE), blessing);
                    removeFromVAccessList(parsedPermissions.get(Permissions.Tags.ADMIN), blessing);
                    break;
                case READ_WRITE_ADMIN:
                    addToVAccessList(parsedPermissions.get(Permissions.Tags.RESOLVE), blessing);
                    addToVAccessList(parsedPermissions.get(Permissions.Tags.READ), blessing);
                    addToVAccessList(parsedPermissions.get(Permissions.Tags.WRITE), blessing);
                    addToVAccessList(parsedPermissions.get(Permissions.Tags.ADMIN), blessing);
                    break;
            }
        }
        return parsedPermissions;
    }
}
