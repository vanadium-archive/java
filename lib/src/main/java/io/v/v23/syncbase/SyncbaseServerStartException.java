package io.v.v23.syncbase;

/**
 * Exception thrown if the syncbase server couldn't be started.
 */
public class SyncbaseServerStartException extends Exception {
    public SyncbaseServerStartException(String msg) {
        super(msg);
    }
}
