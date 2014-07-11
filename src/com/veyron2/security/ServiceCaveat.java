package com.veyron2.security;

import com.veyron2.ipc.VeyronException;

/**
 * ServiceCaveat binds a caveat to a specific set of services.
 */
public class ServiceCaveat implements Caveat {
	private final String service;
	private final Caveat caveat;

	public ServiceCaveat(String service, Caveat caveat) {
		this.service = service;
		this.caveat = caveat;
	}
	// Implements com.veyron2.security.Caveat.
	@Override
	public void validate(Context context) throws VeyronException {
		this.caveat.validate(context);
	}
	/**
	 * Returns the pattern identifying the services this caveat is bound to.
	 *
	 * @return the pattern identifying the services this caveat is bound to.
	 */
	public String getService() { return this.service; }
}