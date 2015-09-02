package io.v.impl.google.services.syncbase.syncbased;

import io.v.v23.V;
import io.v.v23.context.VContext;
import io.v.v23.rpc.Server;
import io.v.v23.syncbase.SyncbaseServerParams;
import io.v.v23.syncbase.SyncbaseServerStartException;
import io.v.v23.verror.VException;

/**
 * An implementation of a syncbase server.
 */
public class SyncbaseServer {
    private static boolean initOnceDone = false;

    private static native void nativeInit() throws VException;
    private static native Server nativeStart(VContext ctx, SyncbaseServerParams params)
            throws VException;

    private static synchronized void initOnce() {
        if (initOnceDone) {
            return;
        }
        V.init();
        try {
            nativeInit();
        } catch (VException e) {
            throw new RuntimeException("Couldn't initialize syncbase native code.", e);
        }
        initOnceDone = true;
    }

    /**
     * Starts the syncbase server with the given parameters.
     * <p>
     * This is a non-blocking call.
     *
     * @param params                         syncbase starting parameters
     * @throws SyncbaseServerStartException  if there was an error starting the syncbase service
     * @return                               vanadium server
     */
    public static Server start(SyncbaseServerParams params) throws SyncbaseServerStartException {
        initOnce();
        VContext ctx = V.init();
        try {
            return nativeStart(ctx, params);
        } catch (VException e) {
            throw new SyncbaseServerStartException(e.getMessage());
        }
    }
}
