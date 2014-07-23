// This file was auto-generated by the veyron vdl tool.
// Source(s):  discharger.vdl revoker.vdl
package com.veyron.services.security.gen_impl;

import com.veyron.services.security.Discharger;
import com.veyron.services.security.DischargerFactory;
import com.veyron.services.security.DischargerService;
import com.veyron.services.security.Revoker;
import com.veyron.services.security.RevokerFactory;
import com.veyron.services.security.RevokerService;
import com.veyron2.ipc.ServerCall;
import com.veyron2.ipc.VeyronException;

public class DischargerServiceWrapper {

	private final DischargerService service;

	public DischargerServiceWrapper(DischargerService service) {
		this.service = service;
	}
	/**
	 * Returns all tags associated with the provided method or null if the method isn't implemented
	 * by this service.
	 */
	public Object[] getMethodTags(ServerCall call, String method) throws VeyronException { 
		if ("discharge".equals(method)) {
			return new Object[]{ new com.veyron2.security.Label(1) };
		}
        if ("getMethodTags".equals(method)) {
            return new Object[]{};
        }
		throw new VeyronException("method: " + method + " not found");
	}
	// Methods from interface Discharger.
	public Object discharge(ServerCall call, Object Caveat) throws VeyronException { 
		return this.service.discharge(call, Caveat);
	}
}
