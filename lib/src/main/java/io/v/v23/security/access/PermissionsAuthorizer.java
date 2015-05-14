// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.security.access;

import io.v.v23.context.VContext;
import io.v.v23.security.Authorizer;
import io.v.v23.security.Blessings;
import io.v.v23.security.Call;
import io.v.v23.security.Security;
import io.v.v23.vdl.Types;
import io.v.v23.vdl.VdlType;
import io.v.v23.vdl.VdlValue;
import io.v.v23.verror.VException;

import java.lang.reflect.Type;
import java.util.Arrays;

public class PermissionsAuthorizer implements Authorizer {
    private final Permissions acls;  // non-null
    private final VdlType tagType;  // non-null

    public static PermissionsAuthorizer create(Permissions acls, Type tagType) throws VException {
        try {
            final VdlType type = Types.getVdlTypeFromReflect(tagType);
            return new PermissionsAuthorizer(acls != null ? acls : new Permissions(), type);
        } catch (IllegalArgumentException e) {
            throw new VException(String.format(
                "Tag type %s does not have a corresponding VdlType: %s", tagType, e.getMessage()));
        }
    }

    private PermissionsAuthorizer(Permissions acls, VdlType tagType) {
        this.acls = acls;
        this.tagType = tagType;
    }

    @Override
    public void authorize(VContext ctx, Call call) throws VException {
        final Blessings local = call.localBlessings();
        final Blessings remote = call.remoteBlessings();
        // Self-RPCs are always authorized.
        if (local != null && local.publicKey() != null &&
                remote != null && remote.publicKey() != null &&
                Arrays.equals(local.publicKey().getEncoded(), remote.publicKey().getEncoded())) {
            return;
        }
        final String[] blessings =
                remote != null ? Security.getRemoteBlessingNames(ctx, call) : new String[0];
        VdlValue[] tags = call.methodTags();
        if (tags == null) {
            tags = new VdlValue[0];
        }
        if (tags.length == 0) {
            throw new VException(String.format("PermissionsAuthorizer.Authorize called with an " +
                    "object (%s, method %s) that has no method tags; this is likely " +
                    "unintentional", call.suffix(), call.method()));
        }
        boolean grant = false;
        for (VdlValue tag : tags) {
            if (tag == null || tag.vdlType() != this.tagType) {
                continue;
            }
            final AccessList acl = this.acls.get(tag.toString());
            if (acl == null || !acl.includes(blessings)) {
                errorACLMatch(blessings);
            }
            grant = true;
        }
        if (!grant) {
            errorACLMatch(blessings);
        }
    }

    private void errorACLMatch(String[] blessings) throws VException {
        throw new VException(String.format("Blessings %s don't match ACL",
            Arrays.asList(blessings).toString()));
    }
}
