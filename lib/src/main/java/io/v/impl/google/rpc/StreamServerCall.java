package io.v.impl.google.rpc;

import org.joda.time.DateTime;
import io.v.v23.security.Blessings;
import io.v.v23.security.Principal;
import io.v.v23.vdl.VdlValue;
import io.v.v23.verror.VException;

import java.io.EOFException;
import java.lang.reflect.Type;

public class StreamServerCall implements io.v.v23.rpc.StreamServerCall {
    private final long nativePtr;
    private final Stream stream;
    private final io.v.v23.security.Call securityCall;

    public native Blessings nativeBlessings(long nativePtr) throws VException;
    private native void nativeFinalize(long nativePtr);

    private StreamServerCall(long nativePtr, Stream stream, io.v.v23.security.Call securityCall) {
        this.nativePtr = nativePtr;
        this.stream = stream;
        this.securityCall = securityCall;
    }
    // Implements io.v.v23.ipc.Stream.
    @Override
    public void send(Object item, Type type) throws VException {
        this.stream.send(item, type);
    }
    @Override
    public Object recv(Type type) throws EOFException, VException {
        return this.stream.recv(type);
    }
    // Implements io.v.v23.ipc.ServerCall.
    @Override
    public Blessings blessings() {
        try {
            return nativeBlessings(this.nativePtr);
        } catch (VException e) {
            throw new RuntimeException("Couldn't get blessings: " + e.getMessage());
        }
    }
    // Implements io.v.v23.security.Call.
    @Override
    public DateTime timestamp() {
        return this.securityCall.timestamp();
    }
    @Override
    public String method() {
        return this.securityCall.method();
    }
    @Override
    public VdlValue[] methodTags() {
        return this.securityCall.methodTags();
    }
    @Override
    public String suffix() {
        return this.securityCall.suffix();
    }
    @Override
    public String localEndpoint() {
        return this.securityCall.localEndpoint();
    }
    @Override
    public String remoteEndpoint() {
        return this.securityCall.remoteEndpoint();
    }
    @Override
    public Principal localPrincipal() {
        return this.securityCall.localPrincipal();
    }
    @Override
    public Blessings localBlessings() {
        return this.securityCall.localBlessings();
        }
    @Override
    public Blessings remoteBlessings() {
        return this.securityCall.remoteBlessings();
    }
    @Override
    public io.v.v23.context.VContext context() {
        return this.securityCall.context();
    }
    // Implements java.lang.Object.
    @Override
    protected void finalize() {
        nativeFinalize(this.nativePtr);
    }
}