// This file was auto-generated by the veyron vdl tool.
// Source: exp.vdl
package com.veyron2.vdl.test_arith.exp;

import com.veyron2.ipc.ServerContext;
import com.veyron2.ipc.VeyronException;
import com.veyron2.vdl.VeyronService;
import com.veyron2.vdl.test_arith.exp.gen_impl.ExpServiceWrapper;

@VeyronService(serviceWrapper=ExpServiceWrapper.class)
public interface ExpService { 
		public double exp(ServerContext context, double x) throws VeyronException;
}