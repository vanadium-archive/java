package io.v.core.veyron2.security;

import org.joda.time.DateTime;

/**
 * Context defines the state available for authorizing a principal.
 */
public interface VContext {
	/**
	 * Returns the timestamp at which the authorization state is to be checked.
	 *
	 * @return timestamp at which the authorization state is to be checked.
	 */
	public DateTime timestamp();

	/**
	 * Returns the method being invoked.
	 *
	 * @return method being invoked.
	 */
	public String method();

	/**
	 * Returns the tags attached to the method, typically through the interface specification
	 * in VDL.  Returns empty array if no tags are attached.
	 *
	 * @return tags attached to the method.
	 */
	public Object[] methodTags();
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
}
