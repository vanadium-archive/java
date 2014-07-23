// This file was auto-generated by the veyron vdl tool.
// Source(s):  node.vdl
package com.veyron.services.mgmt.node.gen_impl;

import com.veyron.services.mgmt.node.Config;
import com.veyron.services.mgmt.node.ConfigFactory;
import com.veyron.services.mgmt.node.ConfigService;
import com.veyron.services.mgmt.node.Node;
import com.veyron.services.mgmt.node.NodeFactory;
import com.veyron.services.mgmt.node.NodeService;
import com.veyron2.ipc.ServerCall;
import com.veyron2.ipc.VeyronException;
import com.veyron2.services.mgmt.binary.Description;
import com.veyron2.services.mgmt.node.ApplicationService;

public class NodeServiceWrapper {

	private final NodeService service;
	private final NodeServiceWrapper node;
	private final ConfigServiceWrapper config;

	public NodeServiceWrapper(NodeService service) {
		this.node = new NodeServiceWrapper(service);
		this.config = new ConfigServiceWrapper(service);
		this.service = service;
	}
	/**
	 * Returns all tags associated with the provided method or null if the method isn't implemented
	 * by this service.
	 */
	public Object[] getMethodTags(ServerCall call, String method) throws VeyronException { 
		try {
			return this.node.getMethodTags(call, method);
		} catch (VeyronException e) {}  // method not found.
		try {
			return this.config.getMethodTags(call, method);
		} catch (VeyronException e) {}  // method not found.
        if ("getMethodTags".equals(method)) {
            return new Object[]{};
        }
		throw new VeyronException("method: " + method + " not found");
	}
	// Methods from interface Node.
	// Methods from sub-interface Node.
	public com.veyron2.services.mgmt.node.Description describe(ServerCall call) throws VeyronException {
		return this.node.describe(call);
	}
	public boolean isRunnable(ServerCall call, Description Description) throws VeyronException {
		return this.node.isRunnable(call, Description);
	}
	public void reset(ServerCall call, long Deadline) throws VeyronException {
		this.node.reset(call, Deadline);
	}
	public String install(ServerCall call, String Name) throws VeyronException {
		return this.node.install(call, Name);
	}
	public void refresh(ServerCall call) throws VeyronException {
		this.node.refresh(call);
	}
	public void restart(ServerCall call) throws VeyronException {
		this.node.restart(call);
	}
	public void resume(ServerCall call) throws VeyronException {
		this.node.resume(call);
	}
	public void revert(ServerCall call) throws VeyronException {
		this.node.revert(call);
	}
	public java.util.ArrayList<String> start(ServerCall call) throws VeyronException {
		return this.node.start(call);
	}
	public void stop(ServerCall call, long Deadline) throws VeyronException {
		this.node.stop(call, Deadline);
	}
	public void suspend(ServerCall call) throws VeyronException {
		this.node.suspend(call);
	}
	public void uninstall(ServerCall call) throws VeyronException {
		this.node.uninstall(call);
	}
	public void update(ServerCall call) throws VeyronException {
		this.node.update(call);
	}
	public void updateTo(ServerCall call, String Name) throws VeyronException {
		this.node.updateTo(call, Name);
	}
	// Methods from sub-interface Config.
	public void set(ServerCall call, String key, String value) throws VeyronException {
		this.config.set(call, key, value);
	}
}
