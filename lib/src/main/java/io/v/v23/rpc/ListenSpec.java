// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.rpc;

/**
 * ListenSpec specifies the information required to create a listening network endpoint for a server
 * and, optionally, the name of a proxy to use in conjunction with that listener.
 */
public class ListenSpec {
    /**
     * Address is a pair of (network protocol, address) that the server should listen on.
     * For TCP, the address must be in {@code ip:port} format. The {@code ip} may be omitted, but
     * the {@code port} can not (choose a port of {@code 0} to have the system allocate one).
     */
    public static class Address {
        private final String protocol;
        private final String address;

        public Address(String protocol, String address) {
            this.protocol = protocol;
            this.address = address;
        }

        /**
         * Returns the network protocol.
          *
          * @return the network protocol.
          */
        public String getProtocol() { return this.protocol; }

        /**
         * Returns the network address.
         *
         * @return the network address.
         */
        public String getAddress() { return this.address; }
    }

    private final Address[] addrs;  // non-null
    private final String proxy;  // non-null
    private final AddressChooser chooser;  // non-null

    public ListenSpec(Address[] addrs, String proxy, AddressChooser chooser) {
        this.addrs = addrs == null ? new Address[0] : addrs;
        this.proxy = proxy == null ? "" : proxy;
        if (chooser == null) {
            throw new IllegalArgumentException(
                    "Cannot instantiate ListenSpec with a null AddressChooser");
        }
        this.chooser = chooser;
    }

    public ListenSpec(Address addr, String proxy, AddressChooser chooser) {
        this(new Address[]{ addr }, proxy, chooser);
    }

    /**
     * Returns the addresses the server should listen on.
     *
     * @return addresses the server should listen on
     */
    public Address[] getAddresses() { return this.addrs; }

    /**
     * Returns the name of the proxy.  If empty, the server isn't proxied.
     *
     * @return the name of the proxy
     */
    public String getProxy() { return this.proxy; }

    /**
     * Returns the address chooser that is used to choose the preferred address
     * to publish with the mount table when one is not otherwise specified.
     *
     * @return the address chooser
     */
    public AddressChooser getChooser() { return this.chooser; }
}