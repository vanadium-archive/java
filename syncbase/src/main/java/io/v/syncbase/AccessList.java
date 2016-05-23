// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.v.v23.security.BlessingPattern;
import io.v.v23.security.access.Constants;
import io.v.v23.security.access.Permissions;

public class AccessList {
    public enum AccessLevel {
        READ,
        READ_WRITE,
        READ_WRITE_ADMIN
    }

    public Map<String, AccessLevel> users;

    private static Set<String> vAccessListToUserIds(io.v.v23.security.access.AccessList accessList) {
        if (!accessList.getNotIn().isEmpty()) {
            throw new RuntimeException("Non-empty not-in section: " + accessList);
        }
        Set<String> res = new HashSet<>();
        for (BlessingPattern bp : accessList.getIn()) {
            res.add(Syncbase.getEmailFromBlessingPattern(bp));
        }
        return res;
    }

    public AccessList() {
        this.users = new HashMap<>();
    }

    protected AccessList(Permissions perms) {
        Set<String> readers = vAccessListToUserIds(perms.get(Constants.READ.getValue()));
        Set<String> writers = vAccessListToUserIds(perms.get(Constants.WRITE.getValue()));
        Set<String> admins = vAccessListToUserIds(perms.get(Constants.ADMIN.getValue()));
        if (!readers.containsAll(writers)) {
            throw new RuntimeException("Some writers are not readers: " + writers + ", " + readers);
        }
        if (!writers.containsAll(admins)) {
            throw new RuntimeException("Some admins are not writers: " + admins + ", " + writers);
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

    private static void addToVAccessList(io.v.v23.security.access.AccessList accessList, BlessingPattern bp) {
        if (!accessList.getIn().contains(bp)) {
            accessList.getIn().add(bp);
        }
    }

    private static void removeFromVAccessList(io.v.v23.security.access.AccessList accessList, BlessingPattern bp) {
        accessList.getIn().remove(bp);
    }

    protected static void applyDelta(Permissions perms, AccessList delta) {
        for (String userId : delta.users.keySet()) {
            AccessLevel level = delta.users.get(userId);
            BlessingPattern bp = Syncbase.getBlessingPatternFromEmail(userId);
            if (level == null) {
                removeFromVAccessList(perms.get(Constants.RESOLVE.getValue()), bp);
                removeFromVAccessList(perms.get(Constants.READ.getValue()), bp);
                removeFromVAccessList(perms.get(Constants.WRITE.getValue()), bp);
                removeFromVAccessList(perms.get(Constants.ADMIN.getValue()), bp);
            } else {
                switch (level) {
                    case READ:
                        addToVAccessList(perms.get(Constants.RESOLVE.getValue()), bp);
                        addToVAccessList(perms.get(Constants.READ.getValue()), bp);
                        removeFromVAccessList(perms.get(Constants.WRITE.getValue()), bp);
                        removeFromVAccessList(perms.get(Constants.ADMIN.getValue()), bp);
                        break;
                    case READ_WRITE:
                        addToVAccessList(perms.get(Constants.RESOLVE.getValue()), bp);
                        addToVAccessList(perms.get(Constants.READ.getValue()), bp);
                        addToVAccessList(perms.get(Constants.WRITE.getValue()), bp);
                        removeFromVAccessList(perms.get(Constants.ADMIN.getValue()), bp);
                        break;
                    case READ_WRITE_ADMIN:
                        addToVAccessList(perms.get(Constants.RESOLVE.getValue()), bp);
                        addToVAccessList(perms.get(Constants.READ.getValue()), bp);
                        addToVAccessList(perms.get(Constants.WRITE.getValue()), bp);
                        addToVAccessList(perms.get(Constants.ADMIN.getValue()), bp);
                        break;
                }
            }
        }
    }
}
