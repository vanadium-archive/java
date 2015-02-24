package io.v.v23.services.security.access;

import io.v.v23.security.Authorizer;
import io.v.v23.security.Blessings;
import io.v.v23.security.VContext;
import io.v.v23.vdl.Types;
import io.v.v23.vdl.VdlType;
import io.v.v23.vdl.VdlValue;
import io.v.v23.verror.VException;

import java.lang.reflect.Type;
import java.util.Arrays;

public class TaggedACLAuthorizer implements Authorizer {
    public static final String TAG = "Veyron runtime";
    private final TaggedACLMap acls;  // non-null
    private final VdlType tagType;  // non-null

    public static TaggedACLAuthorizer create(TaggedACLMap acls, Type tagType) throws VException {
        try {
            final VdlType type = Types.getVdlTypeFromReflect(tagType);
            return new TaggedACLAuthorizer(acls != null ? acls : new TaggedACLMap(), type);
        } catch (IllegalArgumentException e) {
            throw new VException(String.format(
                "Tag type %s does not have a corresponding VdlType: %s", tagType, e.getMessage()));
        }
    }

    private TaggedACLAuthorizer(TaggedACLMap acls, VdlType tagType) {
        this.acls = acls;
        this.tagType = tagType;
    }

    @Override
    public void authorize(VContext context) throws VException {
        final Blessings local = context.localBlessings();
        final Blessings remote = context.remoteBlessings();
        // Self-RPCs are always authorized.
        if (local != null && local.publicKey() != null &&
                remote != null && remote.publicKey() != null &&
                Arrays.equals(local.publicKey().getEncoded(), remote.publicKey().getEncoded())) {
            return;
        }
        final String[] blessings = remote != null ? remote.forContext(context) : new String[0];
        VdlValue[] tags = context.methodTags();
        if (tags == null) {
            tags = new VdlValue[0];
        }
        if (tags.length == 0) {
            throw new VException(String.format("TaggedACLAuthorizer.Authorize called with an " +
                    "object (%s, method %s) that has no method tags; this is likely " +
                    "unintentional", context.suffix(), context.method()));
        }
        boolean grant = false;
        for (VdlValue tag : tags) {
            if (tag == null || tag.vdlType() != this.tagType) {
                continue;
            }
            final ACL acl = this.acls.get(tag.toString());
            if (acl == null || !ACLWrapper.wrap(acl).includes(blessings)) {
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