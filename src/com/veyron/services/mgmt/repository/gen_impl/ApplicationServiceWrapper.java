// This file was auto-generated by the veyron vdl tool.
// Source(s):  repository.vdl
package com.veyron.services.mgmt.repository.gen_impl;

import com.veyron.services.mgmt.repository.Application;
import com.veyron.services.mgmt.repository.ApplicationFactory;
import com.veyron.services.mgmt.repository.ApplicationService;
import com.veyron.services.mgmt.repository.Profile;
import com.veyron.services.mgmt.repository.ProfileFactory;
import com.veyron.services.mgmt.repository.ProfileService;
import com.veyron2.ipc.ServerCall;
import com.veyron2.ipc.VeyronException;
import com.veyron2.services.mgmt.application.Envelope;
import java.util.ArrayList;

public class ApplicationServiceWrapper {

	private final ApplicationService service;
	private final ApplicationServiceWrapper application;

	public ApplicationServiceWrapper(ApplicationService service) {
		this.application = new ApplicationServiceWrapper(service);
		this.service = service;
	}
	/**
	 * Returns all tags associated with the provided method or null if the method isn't implemented
	 * by this service.
	 */
	public Object[] getMethodTags(ServerCall call, String method) throws VeyronException { 
		try {
			return this.application.getMethodTags(call, method);
		} catch (VeyronException e) {}  // method not found.
		if ("put".equals(method)) {
			return new Object[]{ new com.veyron2.security.Label(2) };
		}
		if ("remove".equals(method)) {
			return new Object[]{ new com.veyron2.security.Label(2) };
		}
        if ("getMethodTags".equals(method)) {
            return new Object[]{};
        }
		throw new VeyronException("method: " + method + " not found");
	}
	// Methods from interface Application.
	public void put(ServerCall call, ArrayList<String> Profiles, Envelope Envelope) throws VeyronException { 
		this.service.put(call, Profiles, Envelope);
	}
	public void remove(ServerCall call, String Profile) throws VeyronException { 
		this.service.remove(call, Profile);
	}
	// Methods from sub-interface Application.
	public Envelope match(ServerCall call, ArrayList<String> Profiles) throws VeyronException {
		return this.application.match(call, Profiles);
	}
}
