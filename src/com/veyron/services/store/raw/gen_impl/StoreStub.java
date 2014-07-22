// This file was auto-generated by the veyron vdl tool.
// Source(s):  service.vdl
package com.veyron.services.store.raw.gen_impl;

import com.veyron.services.store.raw.Mutation;
import com.veyron.services.store.raw.Request;
import com.veyron.services.store.raw.Store;
import com.veyron.services.store.raw.StoreFactory;
import com.veyron.services.store.raw.StoreService;
import com.veyron.services.store.raw.VeyronConsts;

/* Client stub for interface: Store. */
public final class StoreStub implements Store {
	private static final java.lang.String vdlIfacePathOpt = "com.veyron.services.store.raw.Store";
	private final com.veyron2.ipc.Client client;
	private final java.lang.String name;

	public StoreStub(com.veyron2.ipc.Client client, java.lang.String name) {
		this.client = client;
		this.name = name;
	}
	// Methods from interface Store.
	@Override
	public com.veyron2.vdl.ClientStream<java.lang.Void,com.veyron2.services.watch.ChangeBatch,java.lang.Void> watch(com.veyron2.ipc.Context context, Request Req) throws com.veyron2.ipc.VeyronException {
		return watch(context, Req, null);
	}
	@Override
	public com.veyron2.vdl.ClientStream<java.lang.Void,com.veyron2.services.watch.ChangeBatch,java.lang.Void> watch(com.veyron2.ipc.Context context, Request Req, com.veyron2.Options veyronOpts) throws com.veyron2.ipc.VeyronException {
		// Prepare input arguments.
		final java.lang.Object[] inArgs = new java.lang.Object[]{ Req };

		// Add VDL path option.
		// NOTE(spetrovic): this option is temporary and will be removed soon after we switch
		// Java to encoding/decoding from vom.Value objects.
		if (veyronOpts == null) veyronOpts = new com.veyron2.Options();
		if (!veyronOpts.has(com.veyron2.OptionDefs.VDL_INTERFACE_PATH)) {
			veyronOpts.set(com.veyron2.OptionDefs.VDL_INTERFACE_PATH, StoreStub.vdlIfacePathOpt);
		}

		// Start the call.
		final com.veyron2.ipc.Client.Call call = this.client.startCall(context, this.name, "Watch", inArgs, veyronOpts);

		return new com.veyron2.vdl.ClientStream<java.lang.Void, com.veyron2.services.watch.ChangeBatch, java.lang.Void>() {
			@Override
			public void send(java.lang.Void item) throws com.veyron2.ipc.VeyronException {
				call.send(item);
			}
			@Override
			public com.veyron2.services.watch.ChangeBatch recv() throws java.io.EOFException, com.veyron2.ipc.VeyronException {
				final com.google.common.reflect.TypeToken<?> type = new com.google.common.reflect.TypeToken<com.veyron2.services.watch.ChangeBatch>() {};
				final java.lang.Object result = call.recv(type);
				try {
					return (com.veyron2.services.watch.ChangeBatch)result;
				} catch (java.lang.ClassCastException e) {
					throw new com.veyron2.ipc.VeyronException("Unexpected result type: " + result.getClass().getCanonicalName());
				}
			}
			@Override
			public java.lang.Void finish() throws com.veyron2.ipc.VeyronException {
				// Prepare output argument and finish the call.
					final com.google.common.reflect.TypeToken<?>[] resultTypes = new com.google.common.reflect.TypeToken<?>[]{  };
					call.finish(resultTypes);
					return null;

			}
		};
	}
	@Override
	public com.veyron2.vdl.ClientStream<Mutation,java.lang.Void,java.lang.Void> putMutations(com.veyron2.ipc.Context context) throws com.veyron2.ipc.VeyronException {
		return putMutations(context, null);
	}
	@Override
	public com.veyron2.vdl.ClientStream<Mutation,java.lang.Void,java.lang.Void> putMutations(com.veyron2.ipc.Context context, com.veyron2.Options veyronOpts) throws com.veyron2.ipc.VeyronException {
		// Prepare input arguments.
		final java.lang.Object[] inArgs = new java.lang.Object[]{  };

		// Add VDL path option.
		// NOTE(spetrovic): this option is temporary and will be removed soon after we switch
		// Java to encoding/decoding from vom.Value objects.
		if (veyronOpts == null) veyronOpts = new com.veyron2.Options();
		if (!veyronOpts.has(com.veyron2.OptionDefs.VDL_INTERFACE_PATH)) {
			veyronOpts.set(com.veyron2.OptionDefs.VDL_INTERFACE_PATH, StoreStub.vdlIfacePathOpt);
		}

		// Start the call.
		final com.veyron2.ipc.Client.Call call = this.client.startCall(context, this.name, "PutMutations", inArgs, veyronOpts);

		return new com.veyron2.vdl.ClientStream<Mutation, java.lang.Void, java.lang.Void>() {
			@Override
			public void send(Mutation item) throws com.veyron2.ipc.VeyronException {
				call.send(item);
			}
			@Override
			public java.lang.Void recv() throws java.io.EOFException, com.veyron2.ipc.VeyronException {
				final com.google.common.reflect.TypeToken<?> type = new com.google.common.reflect.TypeToken<java.lang.Void>() {};
				final java.lang.Object result = call.recv(type);
				try {
					return (java.lang.Void)result;
				} catch (java.lang.ClassCastException e) {
					throw new com.veyron2.ipc.VeyronException("Unexpected result type: " + result.getClass().getCanonicalName());
				}
			}
			@Override
			public java.lang.Void finish() throws com.veyron2.ipc.VeyronException {
				// Prepare output argument and finish the call.
					final com.google.common.reflect.TypeToken<?>[] resultTypes = new com.google.common.reflect.TypeToken<?>[]{  };
					call.finish(resultTypes);
					return null;

			}
		};
	}
}