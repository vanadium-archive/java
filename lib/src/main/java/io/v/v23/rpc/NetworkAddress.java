// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.rpc;

// NetworkAddress represents the network endpoint address.
public class NetworkAddress {
    private final String network, address;

    public NetworkAddress(String network, String address) {
        this.network = network;
        this.address = address;
    }

    /**
     * Returns the description of the network.
     *
     * @return the description of the network
     */
    public String network() { return this.network; }

    /**
     * Returns the network address.
     *
     * @return the network address
     */
    public String address() { return this.address; }

    @Override
    public String toString() {
        return this.address();
    }
}