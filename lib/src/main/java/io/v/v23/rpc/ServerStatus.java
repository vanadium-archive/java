// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.rpc;

import io.v.impl.google.naming.EndpointImpl;
import io.v.v23.naming.Endpoint;
import io.v.v23.verror.VException;
import io.v.v23.rpc.ListenSpec.Address;

import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;


/**
 * The current status of the server.
 */
public class ServerStatus {
    private final ServerState state;
    private final boolean servesMountTable;
    private final MountStatus[] mounts;
    private final String[] endpoints;
    private final Map<Address, VException> lnErrors;
    private final Map<String, VException> proxyErrors;

    /**
     * Creates a new {@link ServerStatus} object.
     *
     * @param  state            the current state of the server
     * @param  servesMountTable whether this server serves a mount table
     * @param  mounts           status of the last mount or unmount operation for every combination
     *                          of name and server address being published by this server
     * @param  endpoints        set of endpoints currently registered with the mount table
     * @param  lnErrors         set of errors currently encountered from listening
     * @param  proxyErrors      set of errors currently encountered from listening on proxies
     */
    public ServerStatus(ServerState state, boolean servesMountTable, MountStatus[] mounts,
            String[] endpoints, Map<Address, VException> lnErrors, Map<String, VException> proxyErrors) {
        this.state = state;
        this.servesMountTable = servesMountTable;
        this.mounts = mounts == null ? new MountStatus[0] : Arrays.copyOf(mounts, mounts.length);
        this.endpoints = endpoints == null ?
                new String[0] : Arrays.copyOf(endpoints, endpoints.length);
        this.lnErrors = lnErrors;
        this.proxyErrors = proxyErrors;
    }

    /**
     * Returns the current state of the server.
     */
    public ServerState getState() {
        return this.state;
    }

    /**
     * Returns true iff this server serves a mount table.
     */
    public boolean servesMountTable() {
        return this.servesMountTable;
    }

    /**
     * Returns the status of the last mount or unmount operation for every combination of name and
     * server address being published by this server.
     */
    public MountStatus[] getMounts() {
        return Arrays.copyOf(this.mounts, this.mounts.length);
    }

    /**
     * Returns the set of endpoints currently registered with the mount table for the names
     * published using this server but excluding those used for serving proxied requests.
     */
    public Endpoint[] getEndpoints() {
        Endpoint[] result = new Endpoint[endpoints.length];
        for (int i = 0; i < endpoints.length; i++) {
            result[i] = EndpointImpl.fromString(endpoints[i]);
        }
        return result;
    }

    /**
     * Returns the map of errors encountered when listening on the network. The returned
     * map is keyed by the {@link Addresses addresses} in the {@link ListenSpec}.
     */
    public Map<Address,VException> getListenErrors() {
       return new HashMap<Address, VException>(this.lnErrors);
    }

    /**
     * Returns the map of errors encountered when listening on the network. The returned
     * map is keyed by the Addresses in the ListenSpec.
     */
    public Map<String,VException> getProxyErrors() {
       return new HashMap<String, VException>(this.proxyErrors);
    }

    @Override
    public String toString() {
        return String.format("State: %s, MountTable: %s, Mounts: %s, Endpoints: %s, ListenErrors: %s, ProxyErrors: %s",
            this.state, this.servesMountTable, Arrays.toString(this.mounts),
            Arrays.toString(this.endpoints), this.lnErrors.toString());
    }
}