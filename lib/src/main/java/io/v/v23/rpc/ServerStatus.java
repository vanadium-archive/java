// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.rpc;

import java.util.Arrays;

/**
 * ServerStatus represents the current status of the server.
 */
public class ServerStatus {
    private ServerState state;
    private final boolean servesMountTable;
    private final MountStatus[] mounts;
    private final String[] endpoints;
    private final ProxyStatus[] proxies;

    /**
     * Creates a new server status object.
     *
     * @param  state            the current state of the server
     * @param  servesMountTable whether this server serves a mount table
     * @param  mounts           status of the last mount or unmount operation for every combination
     *                          of name and server address being published by this server
     * @param  endpoints        set of endpoints currently registered with the mount table
     * @param  proxies          status of all proxy connections maintained by this server
     */
    public ServerStatus(ServerState state, boolean servesMountTable, MountStatus[] mounts,
        String[] endpoints, ProxyStatus[] proxies) {
        this.state = state;
        this.servesMountTable = servesMountTable;
        this.mounts = mounts;
        this.endpoints = endpoints;
        this.proxies = proxies;
    }

    /**
     * Returns the current state of the server.
     *
     * @return the current state of the server
     */
    public ServerState getState() {
        return this.state;
    }

    /**
     * Returns true iff this server serves a mount table.
     *
     * @return true iff this server serves a mount table
     */
    public boolean servesMountTable() {
        return this.servesMountTable;
    }

    /**
     * Returns the status of the last mount or unmount operation for every combination of name and
     * server address being published by this server.
     *
     * @return the status of the last mount or unmount operation for every combination of name and
     *         server address being published by this server
     */
    public MountStatus[] getMounts() {
        return this.mounts;
    }

    /**
     * Returns the set of endpoints currently registered with the mount table for the names
     * published using this server but excluding those used for serving proxied requests.
     *
     * @return the set of endpoints currently registered with the mount table
     */
    public String[] getEndpoints() {
        return this.endpoints;
    }

    /**
     * Returns the status of all proxy connections maintained by this server.
     *
     * @return the status of all proxy connections maintained by this server
     */
    public ProxyStatus[] getProxies() {
        return this.proxies;
    }

    @Override
    public String toString() {
        return String.format("State: %s, MountTable: %s, Mounts: %s, Endpoints: %s, Proxies: %s",
            this.state, this.servesMountTable, Arrays.toString(this.mounts),
            Arrays.toString(this.endpoints), Arrays.toString(this.proxies));
    }
}