package io.v.core.veyron2.ipc;

import io.v.core.veyron2.verror.VException;

/**
 * ProxyStatus represents the status of a proxy connection maintained by a server.
 */
public class ProxyStatus {
    private final String proxy;
    private final String endpoint;
    private final VException error;

    /**
     * Creates a new proxy status object.
     *
     * @param  proxy    name of the proxy
     * @param  endpoint name of the endpoint that the server is using to receive proxied requests on
     * @param  error    any error status of the connection to the proxy
     */
    public ProxyStatus(String proxy, String endpoint, VException error) {
        this.proxy = proxy;
        this.endpoint = endpoint;
        this.error = error;
    }

    /**
     * Returns the name of the proxy.
     *
     * @return the name of the proxy
     */
    public String getProxy() {
        return this.proxy;
    }

    /**
     * Returns the name of the endpoint that the server is using to receive proxied
     * requests on. The endpoint of the proxy itself can be obtained by resolving its name.
     *
     * @return the name of the endpoint that the server is using to receive proxied requests on
     */
    public String getEndpoint() {
        return this.endpoint;
    }

    /**
     * Returns the error status of the connection to the proxy.  It returns {@code null} if the
     * connection is currently correctly established, or the most recent error otherwise.
     *
     * @return the error status of the connection to the proxy
     */
    public VException getError() {
        return this.error;
    }

    @Override
    public String toString() {
        return String.format(
            "Proxy: %s, Endpoint: %s, Error: %s", this.proxy, this.endpoint, this.error);
    }
}
