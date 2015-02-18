package io.v.core.veyron.runtimes.google.ipc;

import org.joda.time.DateTime;
import org.joda.time.Duration;

import io.v.core.veyron2.verror.VException;
import io.v.core.veyron2.context.CancelableVContext;
import io.v.core.veyron2.context.VContext;
import io.v.core.veyron2.security.Blessings;
import io.v.core.veyron2.security.Principal;

import java.io.EOFException;
import java.lang.reflect.Type;
import java.util.concurrent.CountDownLatch;

public class ServerCall implements io.v.core.veyron2.ipc.ServerCall {
    private static final String TAG = "Veyron runtime";

    private final long nativePtr;
    private final Stream stream;
    private final VContext context;
    private final io.v.core.veyron2.security.VContext securityContext;

    public native Blessings nativeBlessings(long nativePtr) throws VException;
    private native void nativeFinalize(long nativePtr);

    private ServerCall(long nativePtr, Stream stream, VContext context,
        io.v.core.veyron2.security.VContext securityContext) {
        this.nativePtr = nativePtr;
        this.stream = stream;
        this.context = context;
        this.securityContext = securityContext;
    }
    // Implements io.v.core.veyron2.ipc.ServerContext.
    @Override
    public Blessings blessings() {
        try {
            return nativeBlessings(this.nativePtr);
        } catch (VException e) {
            android.util.Log.e(TAG, "Couldn't get blessings: " + e.getMessage());
            return null;
        }
    }
    // Implements io.v.core.veyron2.ipc.Stream.
    @Override
    public void send(Object item, Type type) throws VException {
        this.stream.send(item, type);
    }
    @Override
    public Object recv(Type type) throws EOFException, VException {
        return this.stream.recv(type);
    }
    // Implements io.v.core.veyron2.context.VContext.
    @Override
    public DateTime deadline() {
        return this.context.deadline();
    }
    @Override
    public CountDownLatch done() {
        return this.context.done();
    }
    @Override
    public Object value(Object key) {
        return this.context.value(key);
    }
    @Override
    public CancelableVContext withCancel() {
        return this.context.withCancel();
    }
    @Override
    public CancelableVContext withDeadline(DateTime deadline) {
        return this.context.withDeadline(deadline);
    }
    @Override
    public CancelableVContext withTimeout(Duration timeout) {
        return this.context.withTimeout(timeout);
    }
    @Override
    public VContext withValue(Object key, Object value) {
        return this.context.withValue(key, value);
    }
    // Implements io.v.core.veyron2.security.VContext.
    @Override
    public DateTime timestamp() {
        return this.securityContext.timestamp();
    }
    @Override
    public String method() {
        return this.securityContext.method();
    }
    @Override
    public Object[] methodTags() {
        return this.securityContext.methodTags();
    }
    @Override
    public String suffix() {
        return this.securityContext.suffix();
    }
    @Override
    public String localEndpoint() {
        return this.securityContext.localEndpoint();
    }
    @Override
    public String remoteEndpoint() {
        return this.securityContext.remoteEndpoint();
    }
    @Override
    public Principal localPrincipal() {
        return this.securityContext.localPrincipal();
    }
    @Override
    public Blessings localBlessings() {
        return this.securityContext.localBlessings();
        }
    @Override
    public Blessings remoteBlessings() {
        return this.securityContext.remoteBlessings();
    }
    // Implements java.lang.Object.
    @Override
    protected void finalize() {
        nativeFinalize(this.nativePtr);
    }
}