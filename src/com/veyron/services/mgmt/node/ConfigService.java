// This file was auto-generated by the veyron vdl tool.
// Source: node.vdl
package com.veyron.services.mgmt.node;

import com.veyron.services.mgmt.node.gen_impl.ConfigServiceWrapper;
import com.veyron2.ipc.ServerContext;
import com.veyron2.ipc.VeyronException;
import com.veyron2.vdl.VeyronService;

/**
 * Config is an RPC API to the config service.
 */
@VeyronService(serviceWrapper=ConfigServiceWrapper.class)
public interface ConfigService { 
	// Set sets the value for key.
	public void set(ServerContext context, String key, String value) throws VeyronException;
}