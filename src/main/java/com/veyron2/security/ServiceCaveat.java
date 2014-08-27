package com.veyron2.security;

import com.veyron2.ipc.VeyronException;

/**
 * ServiceCaveat binds a caveat to a specific set of services.
 */
public class ServiceCaveat implements Caveat {
	private final BlessingPattern services;
	private final Caveat caveat;

	public ServiceCaveat(BlessingPattern services, Caveat caveat) {
		this.services = services;
		this.caveat = caveat;
	}
	// Implements com.veyron2.security.Caveat.
	@Override
	public void validate(Context context) throws VeyronException {
		this.caveat.validate(context);
	}

	/**
	 * Returns the caveat bound to a set of services.
	 *
	 * @return the caveat.
	 */
	public Caveat getCaveat() { return this.caveat; }

	/**
	 * Returns the pattern identifying the services this caveat is bound to.
	 *
	 * @return the pattern identifying the services this caveat is bound to.
	 */
	public BlessingPattern getServices() { return this.services; }
}
