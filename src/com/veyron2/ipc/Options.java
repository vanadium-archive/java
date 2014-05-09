package com.veyron2.ipc;

/**
 * Options stores various options used across the ipc package.
 */
public class Options {
    /**
     * IDLInterfacePath specifies the IDL interface path that a particular call is targeted at.
     * NOTE(spetrovic): This option is temporary and will be removed soon after we switch
     * Java to encoding/decoding from vom.Value objects.
     */
    public static class IDLInterfacePath implements Client.CallOption {
        private final String path;
        public IDLInterfacePath(String path) {
            this.path = path;
        }
        public String getPath() {
            return this.path;
        }
    }

    /**
     * CallTimeout specifies a timeout for a particular call.
     */
    public static class CallTimeout implements Client.CallOption {
        private final long timeoutMillis;
        public CallTimeout(long timeoutMillis) {
            this.timeoutMillis = timeoutMillis;
        }
        public long getTimeout() {
            return this.timeoutMillis;
        }
    }
}