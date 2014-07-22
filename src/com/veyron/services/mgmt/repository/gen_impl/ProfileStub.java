// This file was auto-generated by the veyron vdl tool.
// Source(s):  repository.vdl
package com.veyron.services.mgmt.repository.gen_impl;

import com.veyron.services.mgmt.repository.Application;
import com.veyron.services.mgmt.repository.ApplicationFactory;
import com.veyron.services.mgmt.repository.ApplicationService;
import com.veyron.services.mgmt.repository.Profile;
import com.veyron.services.mgmt.repository.ProfileFactory;
import com.veyron.services.mgmt.repository.ProfileService;

/* Client stub for interface: Profile. */
public final class ProfileStub implements Profile {
	private static final java.lang.String vdlIfacePathOpt = "com.veyron.services.mgmt.repository.Profile";
	private final com.veyron2.ipc.Client client;
	private final java.lang.String name;
	private final com.veyron2.services.mgmt.repository.Profile profile;

	public ProfileStub(com.veyron2.ipc.Client client, java.lang.String name) {
		this.client = client;
		this.name = name;
		this.profile = new com.veyron2.services.mgmt.repository.gen_impl.ProfileStub(client, name);
	}
	// Methods from interface Profile.
	@Override
	public com.veyron.services.mgmt.profile.Specification specification(com.veyron2.ipc.Context context) throws com.veyron2.ipc.VeyronException {
		return specification(context, null);
	}
	@Override
	public com.veyron.services.mgmt.profile.Specification specification(com.veyron2.ipc.Context context, com.veyron2.Options veyronOpts) throws com.veyron2.ipc.VeyronException {
		// Prepare input arguments.
		final java.lang.Object[] inArgs = new java.lang.Object[]{  };

		// Add VDL path option.
		// NOTE(spetrovic): this option is temporary and will be removed soon after we switch
		// Java to encoding/decoding from vom.Value objects.
		if (veyronOpts == null) veyronOpts = new com.veyron2.Options();
		if (!veyronOpts.has(com.veyron2.OptionDefs.VDL_INTERFACE_PATH)) {
			veyronOpts.set(com.veyron2.OptionDefs.VDL_INTERFACE_PATH, ProfileStub.vdlIfacePathOpt);
		}

		// Start the call.
		final com.veyron2.ipc.Client.Call call = this.client.startCall(context, this.name, "Specification", inArgs, veyronOpts);

		// Prepare output argument and finish the call.
			final com.google.common.reflect.TypeToken<?>[] resultTypes = new com.google.common.reflect.TypeToken<?>[]{ new com.google.common.reflect.TypeToken<com.veyron.services.mgmt.profile.Specification>() {} };
			return (com.veyron.services.mgmt.profile.Specification)call.finish(resultTypes)[0];

	}
	@Override
	public void put(com.veyron2.ipc.Context context, com.veyron.services.mgmt.profile.Specification Specification) throws com.veyron2.ipc.VeyronException {
		put(context, Specification, null);
	}
	@Override
	public void put(com.veyron2.ipc.Context context, com.veyron.services.mgmt.profile.Specification Specification, com.veyron2.Options veyronOpts) throws com.veyron2.ipc.VeyronException {
		// Prepare input arguments.
		final java.lang.Object[] inArgs = new java.lang.Object[]{ Specification };

		// Add VDL path option.
		// NOTE(spetrovic): this option is temporary and will be removed soon after we switch
		// Java to encoding/decoding from vom.Value objects.
		if (veyronOpts == null) veyronOpts = new com.veyron2.Options();
		if (!veyronOpts.has(com.veyron2.OptionDefs.VDL_INTERFACE_PATH)) {
			veyronOpts.set(com.veyron2.OptionDefs.VDL_INTERFACE_PATH, ProfileStub.vdlIfacePathOpt);
		}

		// Start the call.
		final com.veyron2.ipc.Client.Call call = this.client.startCall(context, this.name, "Put", inArgs, veyronOpts);

		// Prepare output argument and finish the call.
			final com.google.common.reflect.TypeToken<?>[] resultTypes = new com.google.common.reflect.TypeToken<?>[]{  };
			call.finish(resultTypes);

	}
	@Override
	public void remove(com.veyron2.ipc.Context context) throws com.veyron2.ipc.VeyronException {
		remove(context, null);
	}
	@Override
	public void remove(com.veyron2.ipc.Context context, com.veyron2.Options veyronOpts) throws com.veyron2.ipc.VeyronException {
		// Prepare input arguments.
		final java.lang.Object[] inArgs = new java.lang.Object[]{  };

		// Add VDL path option.
		// NOTE(spetrovic): this option is temporary and will be removed soon after we switch
		// Java to encoding/decoding from vom.Value objects.
		if (veyronOpts == null) veyronOpts = new com.veyron2.Options();
		if (!veyronOpts.has(com.veyron2.OptionDefs.VDL_INTERFACE_PATH)) {
			veyronOpts.set(com.veyron2.OptionDefs.VDL_INTERFACE_PATH, ProfileStub.vdlIfacePathOpt);
		}

		// Start the call.
		final com.veyron2.ipc.Client.Call call = this.client.startCall(context, this.name, "Remove", inArgs, veyronOpts);

		// Prepare output argument and finish the call.
			final com.google.common.reflect.TypeToken<?>[] resultTypes = new com.google.common.reflect.TypeToken<?>[]{  };
			call.finish(resultTypes);

	}
	// Methods from sub-interface Profile.
	@Override
	public java.lang.String label(com.veyron2.ipc.Context context) throws com.veyron2.ipc.VeyronException {
		return label(context, null);
	}
	@Override
	public java.lang.String label(com.veyron2.ipc.Context context, com.veyron2.Options veyronOpts) throws com.veyron2.ipc.VeyronException {
		// Add VDL path option.
		// NOTE(spetrovic): this option is temporary and will be removed soon after we switch
	    // Java to encoding/decoding from vom.Value objects.
		if (veyronOpts == null) veyronOpts = new com.veyron2.Options();
		if (!veyronOpts.has(com.veyron2.OptionDefs.VDL_INTERFACE_PATH)) {
			veyronOpts.set(com.veyron2.OptionDefs.VDL_INTERFACE_PATH, ProfileStub.vdlIfacePathOpt);
		}
		return this.profile.label(context, veyronOpts);
	}
	@Override
	public java.lang.String description(com.veyron2.ipc.Context context) throws com.veyron2.ipc.VeyronException {
		return description(context, null);
	}
	@Override
	public java.lang.String description(com.veyron2.ipc.Context context, com.veyron2.Options veyronOpts) throws com.veyron2.ipc.VeyronException {
		// Add VDL path option.
		// NOTE(spetrovic): this option is temporary and will be removed soon after we switch
	    // Java to encoding/decoding from vom.Value objects.
		if (veyronOpts == null) veyronOpts = new com.veyron2.Options();
		if (!veyronOpts.has(com.veyron2.OptionDefs.VDL_INTERFACE_PATH)) {
			veyronOpts.set(com.veyron2.OptionDefs.VDL_INTERFACE_PATH, ProfileStub.vdlIfacePathOpt);
		}
		return this.profile.description(context, veyronOpts);
	}
}