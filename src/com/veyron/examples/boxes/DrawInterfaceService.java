// This file was auto-generated by the veyron vdl tool.
// Source: boxes.vdl
package com.veyron.examples.boxes;

import com.veyron.examples.boxes.gen_impl.DrawInterfaceServiceWrapper;
import com.veyron2.ipc.ServerContext;
import com.veyron2.ipc.VeyronException;
import com.veyron2.vdl.Stream;
import com.veyron2.vdl.VeyronService;

/**
 * DrawInterface enables adding a box on another peer
 */
@VeyronService(serviceWrapper=DrawInterfaceServiceWrapper.class)
public interface DrawInterfaceService { 
	// Draw is used to send/receive a stream of boxes to another peer
	public void draw(ServerContext context, Stream<Box,Box> stream) throws VeyronException;
	// SyncBoxes is used to setup a sync service over store to send/receive
// boxes to another peer
	public void syncBoxes(ServerContext context) throws VeyronException;
}