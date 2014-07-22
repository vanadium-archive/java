package com.veyron2.security;

/**
 * Context defines the state available for authorizing a principal.
 */
public interface Context {
	/**
	 * Returns the method being invoked.
	 *
	 * @return method being invoked.
	 */
	public String method();

	/**
	 * Returns the veyron name on which the method is being invoked.
	 *
	 * @return veyron name on which the method is being invoked.
	 */
	public String name();

	/**
	 * Returns the veyron name suffix for the request.
	 *
	 * @return veyron name suffix for the request.
	 */
	public String suffix();

	/**
	 * Returns the method's security label.
	 *
	 * @return method's security label.
	 */
	public Label label();

	/**
	 * Returns the PublicID of the principal at the local end of the request.
	 *
	 * @return PublicID of the principal at the local end of the request.
	 */
	public PublicID localID();

	/**
	 * Returns the PublicID of the principal at the remote end of the request.
	 *
	 * @return PublicID of the principal at the remote end of the request.
	 */
	public PublicID remoteID();

	/**
	 * Returns the endpoint of the principal at the local end of the request.
	 *
	 * @return endpoint of the principal at the local end of the request.
	 */
	public String localEndpoint();

	/**
	 * Returns the endpoint of the principal at the remote end of the request.
	 *
	 * @return endpoint of the principal at the remote end of the request.
	 */
	public String remoteEndpoint();
}
