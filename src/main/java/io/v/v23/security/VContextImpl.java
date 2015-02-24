package io.v.v23.security;

import org.joda.time.DateTime;

import io.v.v23.vdl.VdlValue;
import io.v.v23.verror.VException;

class VContextImpl implements io.v.v23.security.VContext {
    private static final String TAG = "Veyron runtime";

    private final long nativePtr;

    public native DateTime nativeTimestamp(long nativePtr) throws VException;
    public native String nativeMethod(long nativePtr);
    public native VdlValue[] nativeMethodTags(long nativePtr) throws VException;
    private native String nativeSuffix(long nativePtr);
    private native String nativeLocalEndpoint(long nativePtr);
    private native String nativeRemoteEndpoint(long nativePtr);
    private native Principal nativeLocalPrincipal(long nativePtr) throws VException;
    private native Blessings nativeLocalBlessings(long nativePtr) throws VException;
    private native Blessings nativeRemoteBlessings(long nativePtr) throws VException;
    private native void nativeFinalize(long nativePtr);

    VContextImpl(long nativePtr) {
        this.nativePtr = nativePtr;
    }
    // Implements io.v.v23.security.VContext.
    @Override
    public DateTime timestamp() {
        try {
            return nativeTimestamp(this.nativePtr);
        } catch (VException e) {
            android.util.Log.e(TAG, "Couldn't get timestamp: " + e.getMessage());
            return null;
        }
    }
    @Override
    public String method() {
        return nativeMethod(this.nativePtr);
    }
    @Override
    public VdlValue[] methodTags() {
        try {
            final VdlValue[] tags = nativeMethodTags(this.nativePtr);
            return tags != null ? tags : new VdlValue[0];
        } catch (VException e) {
            android.util.Log.e(TAG, "Couldn't get method tags: " + e.getMessage());
            return new VdlValue[0];
        }
    }
    @Override
    public String suffix() {
        return nativeSuffix(this.nativePtr);
    }
    @Override
    public String localEndpoint() {
        return nativeLocalEndpoint(this.nativePtr);
    }
    @Override
    public String remoteEndpoint() {
        return nativeRemoteEndpoint(this.nativePtr);
    }
    @Override
    public Principal localPrincipal() {
        try {
            return nativeLocalPrincipal(this.nativePtr);
        } catch (VException e) {
            android.util.Log.e(TAG, "Couldn't get local Principal: " + e.getMessage());
            return null;
        }
    }
    @Override
    public Blessings localBlessings() {
        try {
            return nativeLocalBlessings(this.nativePtr);
        } catch (VException e) {
            android.util.Log.e(TAG, "Couldn't get local Blessings: " + e.getMessage());
            return null;
        }
    }
    @Override
    public Blessings remoteBlessings() {
        try {
            return nativeRemoteBlessings(this.nativePtr);
        } catch (VException e) {
            android.util.Log.e(TAG, "Couldn't get remote Blessings: " + e.getMessage());
            return null;
        }
    }
    // Implements java.lang.Object.
    @Override
    protected void finalize() {
        nativeFinalize(this.nativePtr);
    }
}