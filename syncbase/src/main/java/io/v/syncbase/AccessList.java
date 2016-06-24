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

    public Map<String, AccessLevel> users;

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

    /**
     * @throws IllegalArgumentException if corePermissions are not valid
     */
    AccessList(Permissions corePermissions) {
        Map<String, Map<String, Set<String>>> parsedPermissions = corePermissions.parse();
        Set<String> resolvers = parsedAccessListToUserIds(parsedPermissions.get(Permissions.Tags.RESOLVE));
        Set<String> readers = parsedAccessListToUserIds(parsedPermissions.get(Permissions.Tags.READ));
        Set<String> writers = parsedAccessListToUserIds(parsedPermissions.get(Permissions.Tags.WRITE));
        Set<String> admins = parsedAccessListToUserIds(parsedPermissions.get(Permissions.Tags.ADMIN));

        if (!readers.containsAll(writers)) {
            throw new IllegalArgumentException("Some readers are not resolvers: " + readers + ", " + resolvers);
        }
        if (!readers.containsAll(writers)) {
            throw new IllegalArgumentException("Some writers are not readers: " + writers + ", " + readers);
        }
        if (!writers.containsAll(admins)) {
            throw new IllegalArgumentException("Some admins are not writers: " + admins + ", " + writers);
        }
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

    /**
     * Computes a new Permissions object based on delta.
     */
    static Permissions applyDelta(Permissions corePermissions, AccessList delta) {
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
        return new Permissions(parsedPermissions);
    }
}
