// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.rpc;

import com.google.common.collect.ImmutableList;

import org.joda.time.DateTime;

import io.v.v23.verror.VException;

/**
 * NetworkChange represents the changes made in response to a network setting change
 * being received.
 */
public class NetworkChange {
    private final DateTime time;
    private final ServerState state;
    private final ImmutableList<String> changedEndpoints;
    private final ImmutableList<NetworkAddress> addedAddrs;
    private final ImmutableList<NetworkAddress> removedAddrs;
    private final VException error;

    public NetworkChange(DateTime time, ServerState state, NetworkAddress[] addedAddrs,
            NetworkAddress[] removedAddrs, String[] changedEndpoints, VException error) {
        this.time = time;
        this.state = state;
        this.addedAddrs = ImmutableList.copyOf(addedAddrs);
        this.removedAddrs = ImmutableList.copyOf(removedAddrs);
        this.changedEndpoints = ImmutableList.copyOf(changedEndpoints);
        this.error = error;
    }

    /**
     * Returns the time of the last change.
     *
     * @return time of the last change
     */
    public DateTime getTime() { return this.time; }

    /**
     * Returns the current state of the server.
     *
     * @return current state of the server
     */
    public ServerState getState() { return this.state; }

    /**
     * Returns the addresses added since the last change.
     *
     * @return list of addresses added since the last change
     */
    public ImmutableList<NetworkAddress> getAddedAddresses() { return this.addedAddrs; }

    /**
     * Returns the addresses removed since the last change.
     *
     * @return list of addresses removed since the last change
     */
    public ImmutableList<NetworkAddress> getRemovedAddresses() { return this.removedAddrs; }

    /**
     * Returns the list of endpoints added/removed as a result of this change.
     *
     * @return list of endpoints added/removed as a result of this change
     */
    public ImmutableList<String> getChangedEndpoints() { return this.changedEndpoints; }

    /**
     * Returns any error encountered.
     *
     * @return any error encountered
     */
    public VException getError() { return this.error; }

    @Override
    public String toString() {
        return String.format("{Time: %s, State: %s, Added addrs: %s, Removed addrs: %s, " +
            "Changed EPs: %s, Error: %s}", this.time, this.state, this.addedAddrs,
            this.removedAddrs, this.changedEndpoints, this.error);
    }
}