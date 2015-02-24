package io.v.v23.ipc;

import io.v.v23.security.Authorizer;

/**
 * ServiceObjectWithAuthorizer is a container holding: (1) a Veyron service object that has
 * invokable methods, and (2) an authorizer that allows control over authorization checks.
 */
public class ServiceObjectWithAuthorizer {
    private final Object service;
    private final Authorizer auth;

    /**
     * Class constructor specifying the Veyron service object and the authorizer that allows
     * control over authorization checks.  A {@code null} authorizer indicates that the
     * default authorization policy should be used.
     *
     * @param  service  the Veyron service object.
     * @param  auth     the authorizer.
     */
    public ServiceObjectWithAuthorizer(Object service, Authorizer auth) {
        this.service = service;
        this.auth = auth;
    }

    /**
     * Returns the Veyron service object.
     *
     * @return the Veyron service object.
     */
    public Object getServiceObject() { return this.service; }

    /**
     * Returns the authorizer.
     *
     * @return the authorizer.
     */
    public Authorizer getAuthorizer() { return this.auth; }
}