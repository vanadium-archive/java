// This file was auto-generated by the veyron vdl tool.
// Source(s):  repository.vdl
package com.veyron2.services.mgmt.repository.gen_impl;

import com.veyron2.ipc.ServerCall;
import com.veyron2.ipc.VeyronException;
import com.veyron2.services.mgmt.application.Envelope;
import com.veyron2.services.mgmt.repository.Application;
import com.veyron2.services.mgmt.repository.ApplicationFactory;
import com.veyron2.services.mgmt.repository.ApplicationService;
import com.veyron2.services.mgmt.repository.Binary;
import com.veyron2.services.mgmt.repository.BinaryFactory;
import com.veyron2.services.mgmt.repository.BinaryService;
import com.veyron2.services.mgmt.repository.Profile;
import com.veyron2.services.mgmt.repository.ProfileFactory;
import com.veyron2.services.mgmt.repository.ProfileService;
import java.util.ArrayList;

public class ApplicationServiceWrapper {

	private final ApplicationService service;

	public ApplicationServiceWrapper(ApplicationService service) {
		this.service = service;
	}
	/**
	 * Returns all tags associated with the provided method or null if the method isn't implemented
	 * by this service.
	 */
	public Object[] getMethodTags(ServerCall call, String method) { 
		if ("match".equals(method)) {
			return new Object[]{ new com.veyron2.security.Label(1) };
		}
		return null;
	}
	// Methods from interface Application.
	public Envelope match(ServerCall call, ArrayList<String> Profiles) throws VeyronException { 
		return this.service.match(call, Profiles);
	}
}