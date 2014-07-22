// This file was auto-generated by the veyron vdl tool.
// Source(s):  arith.vdl advanced.vdl
package com.veyron2.vdl.test_arith.gen_impl;

import com.veyron2.ipc.ServerCall;
import com.veyron2.ipc.VeyronException;
import com.veyron2.vdl.test_arith.AdvancedMath;
import com.veyron2.vdl.test_arith.AdvancedMathFactory;
import com.veyron2.vdl.test_arith.AdvancedMathService;
import com.veyron2.vdl.test_arith.Arith;
import com.veyron2.vdl.test_arith.ArithFactory;
import com.veyron2.vdl.test_arith.ArithService;
import com.veyron2.vdl.test_arith.Calculator;
import com.veyron2.vdl.test_arith.CalculatorFactory;
import com.veyron2.vdl.test_arith.CalculatorService;
import com.veyron2.vdl.test_arith.Trigonometry;
import com.veyron2.vdl.test_arith.TrigonometryFactory;
import com.veyron2.vdl.test_arith.TrigonometryService;
import com.veyron2.vdl.test_arith.VeyronConsts;

public class TrigonometryServiceWrapper {

	private final TrigonometryService service;

	public TrigonometryServiceWrapper(TrigonometryService service) {
		this.service = service;
	}
	/**
	 * Returns all tags associated with the provided method or null if the method isn't implemented
	 * by this service.
	 */
	public Object[] getMethodTags(ServerCall call, String method) { 
		if ("sine".equals(method)) {
			return new Object[]{  };
		}
		if ("cosine".equals(method)) {
			return new Object[]{  };
		}
		return null;
	}
	// Methods from interface Trigonometry.
	public double sine(ServerCall call, double angle) throws VeyronException { 
		return this.service.sine(call, angle);
	}
	public double cosine(ServerCall call, double angle) throws VeyronException { 
		return this.service.cosine(call, angle);
	}
}