package io.v.impl.google.ipc;

import io.v.v23.Options;
import io.v.v23.context.VContext;
import io.v.v23.verror.VException;
import io.v.v23.vom.VomUtil;

import java.lang.reflect.Type;

public class Client implements io.v.v23.ipc.Client {
    private final long nativePtr;

    private native io.v.v23.ipc.Client.Call nativeStartCall(long nativePtr,
        VContext context, String name, String method, byte[][] vomArgs, Options opts)
        throws VException;
    private native void nativeClose(long nativePtr);
    private native void nativeFinalize(long nativePtr);

    private Client(long nativePtr) {
        this.nativePtr = nativePtr;
    }
    // Implement io.v.v23.ipc.Client.
    @Override
    public io.v.v23.ipc.Client.Call startCall(VContext context, String name,
            String method, Object[] args, Type[] argTypes) throws VException {
        return startCall(context, name, method, args, argTypes, null);
    }
    @Override
    public io.v.v23.ipc.Client.Call startCall(VContext context, String name,
            String method, Object[] args, Type[] argTypes, Options opts) throws VException {
        if (opts == null) {
            opts = new Options();
        }
        if ("".equals(method)) {
            throw new VException(String.format("Empty method name invoked on object %s", name));
        }
        if (args.length != argTypes.length) {
            throw new VException(String.format(
                    "Argument count (%d) doesn't match type count (%d) for method %s of object %s",
                    args.length, argTypes.length, name, method));
        }
        // VOM-encode all input arguments, individually.
        final byte[][] vomArgs = new byte[args.length][];
        for (int i = 0; i < args.length; ++i) {
            vomArgs[i] = VomUtil.encode(args[i], argTypes[i]);
        }

        // Invoke native method, making sure that the method name starts with an
        // upper case character.
        method = Character.toUpperCase(method.charAt(0)) + method.substring(1);
        return nativeStartCall(this.nativePtr, context, name, method, vomArgs, opts);
    }
    @Override
    public void close() {
        nativeClose(this.nativePtr);
    }
    // Implement java.lang.Object.
    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null) return false;
        if (this.getClass() != other.getClass()) return false;
        return this.nativePtr == ((Client) other).nativePtr;
    }
    @Override
    public int hashCode() {
        return Long.valueOf(this.nativePtr).hashCode();
    }
    @Override
    protected void finalize() {
        nativeFinalize(this.nativePtr);
    }
}
