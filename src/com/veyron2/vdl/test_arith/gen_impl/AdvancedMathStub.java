// This file was auto-generated by the veyron vdl tool.
// Source(s):  arith.vdl advanced.vdl
package com.veyron2.vdl.test_arith.gen_impl;

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

/* Client stub for interface: AdvancedMath. */
public final class AdvancedMathStub implements AdvancedMath {
	private static final java.lang.String vdlIfacePathOpt = "com.veyron2.vdl.test_arith.AdvancedMath";
	private final com.veyron2.ipc.Client client;
	private final java.lang.String name;
	private final Trigonometry trigonometry;
	private final com.veyron2.vdl.test_arith.exp.Exp exp;

	public AdvancedMathStub(com.veyron2.ipc.Client client, java.lang.String name) {
		this.client = client;
		this.name = name;
		this.trigonometry = new com.veyron2.vdl.test_arith.gen_impl.TrigonometryStub(client, name);
		this.exp = new com.veyron2.vdl.test_arith.exp.gen_impl.ExpStub(client, name);
	}
	// Methods from sub-interface Trigonometry.
	@Override
	public double sine(com.veyron2.ipc.Context context, double angle) throws com.veyron2.ipc.VeyronException {
		return sine(context, angle, null);
	}
	@Override
	public double sine(com.veyron2.ipc.Context context, double angle, com.veyron2.Options veyronOpts) throws com.veyron2.ipc.VeyronException {
		// Add VDL path option.
		// NOTE(spetrovic): this option is temporary and will be removed soon after we switch
	    // Java to encoding/decoding from vom.Value objects.
		if (veyronOpts == null) veyronOpts = new com.veyron2.Options();
		if (!veyronOpts.has(com.veyron2.OptionDefs.VDL_INTERFACE_PATH)) {
			veyronOpts.set(com.veyron2.OptionDefs.VDL_INTERFACE_PATH, AdvancedMathStub.vdlIfacePathOpt);
		}
		return this.trigonometry.sine(context, angle, veyronOpts);
	}
	@Override
	public double cosine(com.veyron2.ipc.Context context, double angle) throws com.veyron2.ipc.VeyronException {
		return cosine(context, angle, null);
	}
	@Override
	public double cosine(com.veyron2.ipc.Context context, double angle, com.veyron2.Options veyronOpts) throws com.veyron2.ipc.VeyronException {
		// Add VDL path option.
		// NOTE(spetrovic): this option is temporary and will be removed soon after we switch
	    // Java to encoding/decoding from vom.Value objects.
		if (veyronOpts == null) veyronOpts = new com.veyron2.Options();
		if (!veyronOpts.has(com.veyron2.OptionDefs.VDL_INTERFACE_PATH)) {
			veyronOpts.set(com.veyron2.OptionDefs.VDL_INTERFACE_PATH, AdvancedMathStub.vdlIfacePathOpt);
		}
		return this.trigonometry.cosine(context, angle, veyronOpts);
	}
	// Methods from sub-interface Exp.
	@Override
	public double exp(com.veyron2.ipc.Context context, double x) throws com.veyron2.ipc.VeyronException {
		return exp(context, x, null);
	}
	@Override
	public double exp(com.veyron2.ipc.Context context, double x, com.veyron2.Options veyronOpts) throws com.veyron2.ipc.VeyronException {
		// Add VDL path option.
		// NOTE(spetrovic): this option is temporary and will be removed soon after we switch
	    // Java to encoding/decoding from vom.Value objects.
		if (veyronOpts == null) veyronOpts = new com.veyron2.Options();
		if (!veyronOpts.has(com.veyron2.OptionDefs.VDL_INTERFACE_PATH)) {
			veyronOpts.set(com.veyron2.OptionDefs.VDL_INTERFACE_PATH, AdvancedMathStub.vdlIfacePathOpt);
		}
		return this.exp.exp(context, x, veyronOpts);
	}
}