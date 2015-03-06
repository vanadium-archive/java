package io.v.impl.google.ipc;

import io.v.v23.verror.VException;
import io.v.v23.vom.VomUtil;

import java.io.EOFException;
import java.lang.reflect.Type;

public class Stream implements io.v.v23.ipc.Stream {
    private final long nativePtr;

    private native void nativeSend(long nativePtr, byte[] vomItem) throws VException;
    private native byte[] nativeRecv(long nativePtr) throws EOFException, VException;
    private native void nativeFinalize(long nativePtr);

    private Stream(long nativePtr) {
        this.nativePtr = nativePtr;
    }
    @Override
    public void send(Object item, Type type) throws VException {
        final byte[] vomItem = VomUtil.encode(item, type);
        nativeSend(nativePtr, vomItem);
    }

    @Override
    public Object recv(Type type) throws EOFException, VException {
        final byte[] result = nativeRecv(nativePtr);
        return VomUtil.decode(result, type);
    }
    @Override
    protected void finalize() {
        nativeFinalize(this.nativePtr);
    }
}
