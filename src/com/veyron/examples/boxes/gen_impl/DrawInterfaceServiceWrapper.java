// This file was auto-generated by the veyron vdl tool.
// Source(s):  boxes.vdl
package com.veyron.examples.boxes.gen_impl;

import com.google.common.reflect.TypeToken;
import com.veyron.examples.boxes.Box;
import com.veyron.examples.boxes.BoxSignalling;
import com.veyron.examples.boxes.BoxSignallingFactory;
import com.veyron.examples.boxes.BoxSignallingService;
import com.veyron.examples.boxes.DrawInterface;
import com.veyron.examples.boxes.DrawInterfaceFactory;
import com.veyron.examples.boxes.DrawInterfaceService;
import com.veyron2.ipc.ServerCall;
import com.veyron2.ipc.VeyronException;
import com.veyron2.vdl.Stream;

public class DrawInterfaceServiceWrapper {

	private final DrawInterfaceService service;

	public DrawInterfaceServiceWrapper(DrawInterfaceService service) {
		this.service = service;
	}
	/**
	 * Returns all tags associated with the provided method or null if the method isn't implemented
	 * by this service.
	 */
	public Object[] getMethodTags(ServerCall call, String method) throws VeyronException { 
		if ("draw".equals(method)) {
			return new Object[]{  };
		}
		if ("syncBoxes".equals(method)) {
			return new Object[]{  };
		}
        if ("getMethodTags".equals(method)) {
            return new Object[]{};
        }
		throw new VeyronException("method: " + method + " not found");
	}
	// Methods from interface DrawInterface.
	public void draw(ServerCall call) throws VeyronException { 
		final ServerCall serverCall = call;
		final Stream<Box,Box> stream = new Stream<Box,Box>() {
			@Override
			public void send(Box item) throws VeyronException {
				serverCall.send(item);
			}
			@Override
			public Box recv() throws java.io.EOFException, VeyronException {
				final TypeToken<?> type = new TypeToken<Box>() {};
				final Object result = serverCall.recv(type);
				try {
					return (Box)result;
				} catch (java.lang.ClassCastException e) {
					throw new VeyronException("Unexpected result type: " + result.getClass().getCanonicalName());
				}
			}
		};
		this.service.draw(call, stream);
	}
	public void syncBoxes(ServerCall call) throws VeyronException { 
		this.service.syncBoxes(call);
	}
}
