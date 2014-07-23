// This file was auto-generated by the veyron vdl tool.
// Source(s):  service.vdl
package com.veyron2.services.security.access.gen_impl;

import com.veyron2.ipc.ServerCall;
import com.veyron2.ipc.VeyronException;
import com.veyron2.services.security.access.ACL;
import com.veyron2.services.security.access.Entries;
import com.veyron2.services.security.access.Group;
import com.veyron2.services.security.access.ObjectFactory;
import com.veyron2.services.security.access.ObjectService;

public class ObjectServiceWrapper {

	private final ObjectService service;

	public ObjectServiceWrapper(ObjectService service) {
		this.service = service;
	}
	/**
	 * Returns all tags associated with the provided method or null if the method isn't implemented
	 * by this service.
	 */
	public Object[] getMethodTags(ServerCall call, String method) throws VeyronException { 
		if ("setACL".equals(method)) {
			return new Object[]{ new com.veyron2.security.Label(4) };
		}
		if ("getACL".equals(method)) {
			return new Object[]{ new com.veyron2.security.Label(4) };
		}
        if ("getMethodTags".equals(method)) {
            return new Object[]{};
        }
		throw new VeyronException("method: " + method + " not found");
	}
	// Methods from interface Object.
	public void setACL(ServerCall call, ACL acl, String etag) throws VeyronException { 
		this.service.setACL(call, acl, etag);
	}
	public ObjectService.GetACLOut getACL(ServerCall call) throws VeyronException { 
		return this.service.getACL(call);
	}
}
