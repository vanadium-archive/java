package io.veyron.veyron.veyron2.ipc;

/**
 * ListenSpec specifies the information required to create a listening network endpoint for a server
 * and, optionally, the name of a proxy to use in conjunction with that listener.
 */
public class ListenSpec {
	public static ListenSpec DEFAULT = new ListenSpec("tcp", "proxy.envyor.com:8100");
	private final String protocol;
	private final String proxy;

	public ListenSpec(String protocol, String proxy) {
		this.protocol = protocol;
		this.proxy = proxy;
	}

	/**
	 * Returns the network protocol.
	 *
	 * @return the network protocol.
	 */
	public String getProtocol() { return this.protocol; }

	/**
	 * Returns the address of the proxy.  If empty, the server isn't proxied.
	 *
	 * @return the address of the proxy.
	 */
	public String getProxy() { return this.proxy; }
}