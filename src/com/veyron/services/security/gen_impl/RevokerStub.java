// This file was auto-generated by the veyron vdl tool.
// Source(s):  discharger.vdl revoker.vdl
package com.veyron.services.security.gen_impl;

import com.veyron.services.security.Discharger;
import com.veyron.services.security.DischargerFactory;
import com.veyron.services.security.DischargerService;
import com.veyron.services.security.Revoker;
import com.veyron.services.security.RevokerFactory;
import com.veyron.services.security.RevokerService;

/* Client stub for interface: Revoker. */
public final class RevokerStub implements Revoker {
	private static final java.lang.String vdlIfacePathOpt = "com.veyron.services.security.Revoker";
	private final com.veyron2.ipc.Client client;
	private final java.lang.String name;

	public RevokerStub(com.veyron2.ipc.Client client, java.lang.String name) {
		this.client = client;
		this.name = name;
	}
	// Methods from interface Revoker.
	@Override
	public void revoke(com.veyron2.ipc.Context context, com.veyron.services.security.RevocationToken caveatPreimage) throws com.veyron2.ipc.VeyronException {
		revoke(context, caveatPreimage, null);
	}
	@Override
	public void revoke(com.veyron2.ipc.Context context, com.veyron.services.security.RevocationToken caveatPreimage, com.veyron2.Options veyronOpts) throws com.veyron2.ipc.VeyronException {
		// Prepare input arguments.
		final java.lang.Object[] inArgs = new java.lang.Object[]{ caveatPreimage };

		// Add VDL path option.
		// NOTE(spetrovic): this option is temporary and will be removed soon after we switch
		// Java to encoding/decoding from vom.Value objects.
		if (veyronOpts == null) veyronOpts = new com.veyron2.Options();
		if (!veyronOpts.has(com.veyron2.OptionDefs.VDL_INTERFACE_PATH)) {
			veyronOpts.set(com.veyron2.OptionDefs.VDL_INTERFACE_PATH, RevokerStub.vdlIfacePathOpt);
		}

		// Start the call.
		final com.veyron2.ipc.Client.Call call = this.client.startCall(context, this.name, "Revoke", inArgs, veyronOpts);

		// Prepare output argument and finish the call.
			final com.google.common.reflect.TypeToken<?>[] resultTypes = new com.google.common.reflect.TypeToken<?>[]{  };
			call.finish(resultTypes);

	}
}
