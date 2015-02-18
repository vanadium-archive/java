package io.v.core.veyron.runtimes.google.ipc;

import io.v.core.veyron2.verror.VException;
import io.v.core.veyron2.util.VomUtil;

import java.io.EOFException;
import java.lang.reflect.Type;

public class Call implements io.v.core.veyron2.ipc.Client.Call {
    private final long nativePtr;
    private final io.v.core.veyron2.ipc.Stream stream;

    private native void nativeCloseSend() throws VException;
    private native byte[][] nativeFinish(long nativePtr, int numResults) throws VException;
    private native void nativeFinalize(long nativePtr);

    private Call(long nativePtr, Stream stream) {
        this.nativePtr = nativePtr;
        this.stream = stream;
    }

    // Implements io.v.core.veyron2.ipc.Client.Call.
    @Override
    public void closeSend() throws VException {
        nativeCloseSend();
    }
    @Override
    public Object[] finish(Type[] types) throws VException {
        final byte[][] vomResults = nativeFinish(this.nativePtr, types.length);
        if (vomResults.length != types.length) {
            throw new VException(String.format(
                "Mismatch in number of results, want %s, have %s",
                types.length, vomResults.length));
        }
        // VOM-decode results.
        final Object[] ret = new Object[types.length];
        for (int i = 0; i < types.length; i++) {
            ret[i] = VomUtil.decode(vomResults[i], types[i]);
        }
        return ret;
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
    // Implements java.lang.Object.
    @Override
    protected void finalize() {
        nativeFinalize(this.nativePtr);
    }
}