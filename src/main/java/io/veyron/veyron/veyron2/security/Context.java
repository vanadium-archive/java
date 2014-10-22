package io.veyron.veyron.veyron2.security;

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
	 * Returns the principal used to authenticate to the remote end.
	 *
	 * @return the principal used to authenticate to the remote end.
	 */
	public Principal localPrincipal();

	/**
	 * Returns the blessings sent to the remote end for authentication.
	 *
	 * @return the blessings sent to the remote end for authentication.
	 */
	public Blessings localBlessings();

	/**
	 * Returns the blessings received from the remote end during authentication.
	 *
	 * @return [description]
	 */
	public Blessings remoteBlessings();

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

	/**
	 * Returns the PublicID of the principal at the local end of the request.
	 *
	 * @deprecated Replace by localBlessings.
	 *
	 * @return PublicID of the principal at the local end of the request.
	 */
	@Deprecated
	public PublicID localID();

	/**
	 * Returns the PublicID of the principal at the remote end of the request.
	 *
	 * @deprecated Replaced by remoteBlessings.
	 *
	 * @return PublicID of the principal at the remote end of the request.
	 */
	@Deprecated
	public PublicID remoteID();
}
