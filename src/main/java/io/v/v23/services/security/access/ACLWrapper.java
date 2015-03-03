package io.v.v23.services.security.access;

import io.v.v23.security.Authorizer;
import io.v.v23.security.Call;
import io.v.v23.verror.VException;

public class ACLWrapper implements Authorizer {
    private static native ACLWrapper nativeWrap(ACL acl) throws VException;

    /**
     * Wraps the provided ACL.
     *
     * @param  acl             ACL being wrapped.
     * @return                 wrapped ACL.
     * @throws VException      if the ACL couldn't be wrapped.
     */
    public static ACLWrapper wrap(ACL acl) throws VException {
        return nativeWrap(acl);
    }

    private native boolean nativeIncludes(long nativePtr, String[] blessings);
    private native void nativeAuthorize(long nativePtr, Call call);
    private native void nativeFinalize(long nativePtr);

    private long nativePtr;
    private ACL acl;

    private ACLWrapper(long nativePtr, ACL acl) {
        this.nativePtr = nativePtr;
        this.acl = acl;
    }

    /**
     * Returns true iff the ACL grants access to a principal that presents these blessings.
     *
     * @param  blessings blessings we are getting access for.
     * @return           true iff the ACL grants access to a principal that presents these
     *                   blessings.
     */
    public boolean includes(String... blessings) {
        return nativeIncludes(this.nativePtr, blessings);
    }

    /**
     * Implements {@code Authorizer} where the request is authorized only if the remote blessings
     * are included in the ACL.
     *
     * @param  call       the call being authorized
     * @throws VException if the request is not authorized
     */
    @Override
    public void authorize(Call call) throws VException {
        nativeAuthorize(this.nativePtr, call);
    }

    /*
     * Returns the ACL contained in the wrapper.
     *
     * @return the ACL contained in the wrapper.
     */
    public ACL getACL() {
        return this.acl;
    }

    @Override
    protected void finalize() {
        nativeFinalize(this.nativePtr);
    }
}