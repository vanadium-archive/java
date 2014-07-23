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

/* Client stub for interface: Calculator. */
public final class CalculatorStub implements Calculator {
	private static final java.lang.String vdlIfacePathOpt = "com.veyron2.vdl.test_arith.Calculator";
	private final com.veyron2.ipc.Client client;
	private final java.lang.String name;
	private final Arith arith;
	private final AdvancedMath advancedMath;

	public CalculatorStub(com.veyron2.ipc.Client client, java.lang.String name) {
		this.client = client;
		this.name = name;
		this.arith = new com.veyron2.vdl.test_arith.gen_impl.ArithStub(client, name);
		this.advancedMath = new com.veyron2.vdl.test_arith.gen_impl.AdvancedMathStub(client, name);
	}
	// Methods from interface Calculator.
	@Override
	public void on(com.veyron2.ipc.Context context) throws com.veyron2.ipc.VeyronException {
		on(context, null);
	}
	@Override
	public void on(com.veyron2.ipc.Context context, com.veyron2.Options veyronOpts) throws com.veyron2.ipc.VeyronException {
		// Prepare input arguments.
		final java.lang.Object[] inArgs = new java.lang.Object[]{  };

		// Add VDL path option.
		// NOTE(spetrovic): this option is temporary and will be removed soon after we switch
		// Java to encoding/decoding from vom.Value objects.
		if (veyronOpts == null) veyronOpts = new com.veyron2.Options();
		if (!veyronOpts.has(com.veyron2.OptionDefs.VDL_INTERFACE_PATH)) {
			veyronOpts.set(com.veyron2.OptionDefs.VDL_INTERFACE_PATH, CalculatorStub.vdlIfacePathOpt);
		}

		// Start the call.
		final com.veyron2.ipc.Client.Call call = this.client.startCall(context, this.name, "On", inArgs, veyronOpts);

		// Prepare output argument and finish the call.
			final com.google.common.reflect.TypeToken<?>[] resultTypes = new com.google.common.reflect.TypeToken<?>[]{  };
			call.finish(resultTypes);

	}
	@Override
	public void off(com.veyron2.ipc.Context context) throws com.veyron2.ipc.VeyronException {
		off(context, null);
	}
	@Override
	public void off(com.veyron2.ipc.Context context, com.veyron2.Options veyronOpts) throws com.veyron2.ipc.VeyronException {
		// Prepare input arguments.
		final java.lang.Object[] inArgs = new java.lang.Object[]{  };

		// Add VDL path option.
		// NOTE(spetrovic): this option is temporary and will be removed soon after we switch
		// Java to encoding/decoding from vom.Value objects.
		if (veyronOpts == null) veyronOpts = new com.veyron2.Options();
		if (!veyronOpts.has(com.veyron2.OptionDefs.VDL_INTERFACE_PATH)) {
			veyronOpts.set(com.veyron2.OptionDefs.VDL_INTERFACE_PATH, CalculatorStub.vdlIfacePathOpt);
		}

		// Start the call.
		final com.veyron2.ipc.Client.Call call = this.client.startCall(context, this.name, "Off", inArgs, veyronOpts);

		// Prepare output argument and finish the call.
			final com.google.common.reflect.TypeToken<?>[] resultTypes = new com.google.common.reflect.TypeToken<?>[]{  };
			call.finish(resultTypes);

	}
	// Methods from sub-interface Arith.
	@Override
	public int add(com.veyron2.ipc.Context context, int a, int b) throws com.veyron2.ipc.VeyronException {
		return add(context, a, b, null);
	}
	@Override
	public int add(com.veyron2.ipc.Context context, int a, int b, com.veyron2.Options veyronOpts) throws com.veyron2.ipc.VeyronException {
		// Add VDL path option.
		// NOTE(spetrovic): this option is temporary and will be removed soon after we switch
	    // Java to encoding/decoding from vom.Value objects.
		if (veyronOpts == null) veyronOpts = new com.veyron2.Options();
		if (!veyronOpts.has(com.veyron2.OptionDefs.VDL_INTERFACE_PATH)) {
			veyronOpts.set(com.veyron2.OptionDefs.VDL_INTERFACE_PATH, CalculatorStub.vdlIfacePathOpt);
		}
		return this.arith.add(context, a, b, veyronOpts);
	}
	@Override
	public Arith.DivModOut divMod(com.veyron2.ipc.Context context, int a, int b) throws com.veyron2.ipc.VeyronException {
		return divMod(context, a, b, null);
	}
	@Override
	public Arith.DivModOut divMod(com.veyron2.ipc.Context context, int a, int b, com.veyron2.Options veyronOpts) throws com.veyron2.ipc.VeyronException {
		// Add VDL path option.
		// NOTE(spetrovic): this option is temporary and will be removed soon after we switch
	    // Java to encoding/decoding from vom.Value objects.
		if (veyronOpts == null) veyronOpts = new com.veyron2.Options();
		if (!veyronOpts.has(com.veyron2.OptionDefs.VDL_INTERFACE_PATH)) {
			veyronOpts.set(com.veyron2.OptionDefs.VDL_INTERFACE_PATH, CalculatorStub.vdlIfacePathOpt);
		}
		return this.arith.divMod(context, a, b, veyronOpts);
	}
	@Override
	public int sub(com.veyron2.ipc.Context context, com.veyron2.vdl.test_base.Args args) throws com.veyron2.ipc.VeyronException {
		return sub(context, args, null);
	}
	@Override
	public int sub(com.veyron2.ipc.Context context, com.veyron2.vdl.test_base.Args args, com.veyron2.Options veyronOpts) throws com.veyron2.ipc.VeyronException {
		// Add VDL path option.
		// NOTE(spetrovic): this option is temporary and will be removed soon after we switch
	    // Java to encoding/decoding from vom.Value objects.
		if (veyronOpts == null) veyronOpts = new com.veyron2.Options();
		if (!veyronOpts.has(com.veyron2.OptionDefs.VDL_INTERFACE_PATH)) {
			veyronOpts.set(com.veyron2.OptionDefs.VDL_INTERFACE_PATH, CalculatorStub.vdlIfacePathOpt);
		}
		return this.arith.sub(context, args, veyronOpts);
	}
	@Override
	public int mul(com.veyron2.ipc.Context context, com.veyron2.vdl.test_base.NestedArgs nested) throws com.veyron2.ipc.VeyronException {
		return mul(context, nested, null);
	}
	@Override
	public int mul(com.veyron2.ipc.Context context, com.veyron2.vdl.test_base.NestedArgs nested, com.veyron2.Options veyronOpts) throws com.veyron2.ipc.VeyronException {
		// Add VDL path option.
		// NOTE(spetrovic): this option is temporary and will be removed soon after we switch
	    // Java to encoding/decoding from vom.Value objects.
		if (veyronOpts == null) veyronOpts = new com.veyron2.Options();
		if (!veyronOpts.has(com.veyron2.OptionDefs.VDL_INTERFACE_PATH)) {
			veyronOpts.set(com.veyron2.OptionDefs.VDL_INTERFACE_PATH, CalculatorStub.vdlIfacePathOpt);
		}
		return this.arith.mul(context, nested, veyronOpts);
	}
	@Override
	public void genError(com.veyron2.ipc.Context context) throws com.veyron2.ipc.VeyronException {
		genError(context, null);
	}
	@Override
	public void genError(com.veyron2.ipc.Context context, com.veyron2.Options veyronOpts) throws com.veyron2.ipc.VeyronException {
		// Add VDL path option.
		// NOTE(spetrovic): this option is temporary and will be removed soon after we switch
	    // Java to encoding/decoding from vom.Value objects.
		if (veyronOpts == null) veyronOpts = new com.veyron2.Options();
		if (!veyronOpts.has(com.veyron2.OptionDefs.VDL_INTERFACE_PATH)) {
			veyronOpts.set(com.veyron2.OptionDefs.VDL_INTERFACE_PATH, CalculatorStub.vdlIfacePathOpt);
		}
		this.arith.genError(context, veyronOpts);
	}
	@Override
	public com.veyron2.vdl.ClientStream<java.lang.Void,java.lang.Integer,java.lang.Void> count(com.veyron2.ipc.Context context, int Start) throws com.veyron2.ipc.VeyronException {
		return count(context, Start, null);
	}
	@Override
	public com.veyron2.vdl.ClientStream<java.lang.Void,java.lang.Integer,java.lang.Void> count(com.veyron2.ipc.Context context, int Start, com.veyron2.Options veyronOpts) throws com.veyron2.ipc.VeyronException {
		// Add VDL path option.
		// NOTE(spetrovic): this option is temporary and will be removed soon after we switch
	    // Java to encoding/decoding from vom.Value objects.
		if (veyronOpts == null) veyronOpts = new com.veyron2.Options();
		if (!veyronOpts.has(com.veyron2.OptionDefs.VDL_INTERFACE_PATH)) {
			veyronOpts.set(com.veyron2.OptionDefs.VDL_INTERFACE_PATH, CalculatorStub.vdlIfacePathOpt);
		}
		return this.arith.count(context, Start, veyronOpts);
	}
	@Override
	public com.veyron2.vdl.ClientStream<java.lang.Integer,java.lang.Integer,java.lang.Integer> streamingAdd(com.veyron2.ipc.Context context) throws com.veyron2.ipc.VeyronException {
		return streamingAdd(context, null);
	}
	@Override
	public com.veyron2.vdl.ClientStream<java.lang.Integer,java.lang.Integer,java.lang.Integer> streamingAdd(com.veyron2.ipc.Context context, com.veyron2.Options veyronOpts) throws com.veyron2.ipc.VeyronException {
		// Add VDL path option.
		// NOTE(spetrovic): this option is temporary and will be removed soon after we switch
	    // Java to encoding/decoding from vom.Value objects.
		if (veyronOpts == null) veyronOpts = new com.veyron2.Options();
		if (!veyronOpts.has(com.veyron2.OptionDefs.VDL_INTERFACE_PATH)) {
			veyronOpts.set(com.veyron2.OptionDefs.VDL_INTERFACE_PATH, CalculatorStub.vdlIfacePathOpt);
		}
		return this.arith.streamingAdd(context, veyronOpts);
	}
	@Override
	public java.lang.Object quoteAny(com.veyron2.ipc.Context context, java.lang.Object a) throws com.veyron2.ipc.VeyronException {
		return quoteAny(context, a, null);
	}
	@Override
	public java.lang.Object quoteAny(com.veyron2.ipc.Context context, java.lang.Object a, com.veyron2.Options veyronOpts) throws com.veyron2.ipc.VeyronException {
		// Add VDL path option.
		// NOTE(spetrovic): this option is temporary and will be removed soon after we switch
	    // Java to encoding/decoding from vom.Value objects.
		if (veyronOpts == null) veyronOpts = new com.veyron2.Options();
		if (!veyronOpts.has(com.veyron2.OptionDefs.VDL_INTERFACE_PATH)) {
			veyronOpts.set(com.veyron2.OptionDefs.VDL_INTERFACE_PATH, CalculatorStub.vdlIfacePathOpt);
		}
		return this.arith.quoteAny(context, a, veyronOpts);
	}
	// Methods from sub-interface AdvancedMath.
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
			veyronOpts.set(com.veyron2.OptionDefs.VDL_INTERFACE_PATH, CalculatorStub.vdlIfacePathOpt);
		}
		return this.advancedMath.sine(context, angle, veyronOpts);
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
			veyronOpts.set(com.veyron2.OptionDefs.VDL_INTERFACE_PATH, CalculatorStub.vdlIfacePathOpt);
		}
		return this.advancedMath.cosine(context, angle, veyronOpts);
	}
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
			veyronOpts.set(com.veyron2.OptionDefs.VDL_INTERFACE_PATH, CalculatorStub.vdlIfacePathOpt);
		}
		return this.advancedMath.exp(context, x, veyronOpts);
	}
}
