package io.veyron.veyron.veyron2.ipc;

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
	public class Address {
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

	private final Address[] addrs;
	private final String proxy;

	public ListenSpec(Address[] addrs, String proxy) {
		this.addrs = addrs;
		this.proxy = proxy;
	}

	/**
	 * Returns the addresses the server should listen on.
	 *
	 * @return addresses the server should listen on.
	 */
	public Address[] getAddresses() { return this.addrs; }

	/**
	 * Returns the name of the proxy.  If empty, the server isn't proxied.
	 *
	 * @return the name of the proxy.
	 */
	public String getProxy() { return this.proxy; }
}