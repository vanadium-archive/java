package io.veyron.veyron.veyron2.security;

import io.veyron.veyron.veyron2.VeyronException;

public class ACLWrapper {
	private static final String TAG = "Veyron runtime";

	private static native ACLWrapper nativeWrap(io.veyron.veyron.veyron.security.acl.ACL acl)
			throws VeyronException;

	/**
	 * Wraps the provided ACL.
	 *
	 * @param  acl             ACL being wrapped.
	 * @return                 wrapped ACL.
	 * @throws VeyronException if the ACL couldn't be wrapped.
	 */
	public static ACLWrapper wrap(io.veyron.veyron.veyron.security.acl.ACL acl)
			throws VeyronException {
		return nativeWrap(acl);
	}

	private native boolean nativeIncludes(long nativePtr, String[] blessings);
	private native void nativeFinalize(long nativePtr);


	private long nativePtr;
	private io.veyron.veyron.veyron.security.acl.ACL acl;

	private ACLWrapper(long nativePtr, io.veyron.veyron.veyron.security.acl.ACL acl) {
		this.nativePtr = nativePtr;
		this.acl = acl;
	}

	/**
	 * Returns true iff the ACL grants access to a principal that presents these blessings.
	 *
	 * @param  blessings blessings we are getting access for.
	 * @return           true iff the ACL grants access to a principal that presents these blessings.
	 */
	public boolean includes(String... blessings) {
		return nativeIncludes(this.nativePtr, blessings);
	}

	/*
	 * Returns the ACL contained in the wrapper.
	 *
	 * @return the ACL contained in the wrapper.
	 */
	public io.veyron.veyron.veyron.security.acl.ACL getACL() {
		return this.acl;
	}

	@Override
	protected void finalize() {
		nativeFinalize(this.nativePtr);
	}
}