// This file was auto-generated by the veyron vdl tool.
// Source(s):  service.vdl
package com.veyron2.services.store.gen_impl;

import com.veyron2.services.store.Conflict;
import com.veyron2.services.store.Entry;
import com.veyron2.services.store.Object;
import com.veyron2.services.store.ObjectFactory;
import com.veyron2.services.store.ObjectService;
import com.veyron2.services.store.QueryResult;
import com.veyron2.services.store.Stat;
import com.veyron2.services.store.Store;
import com.veyron2.services.store.StoreFactory;
import com.veyron2.services.store.StoreService;
import com.veyron2.services.store.VeyronConsts;

/* Client stub for interface: Store. */
public final class StoreStub implements Store {
	private static final java.lang.String vdlIfacePathOpt = "com.veyron2.services.store.Store";
	private final com.veyron2.ipc.Client client;
	private final java.lang.String name;

	public StoreStub(com.veyron2.ipc.Client client, java.lang.String name) {
		this.client = client;
		this.name = name;
	}
	// Methods from interface Store.
	@Override
	public void createTransaction(com.veyron2.ipc.Context context, com.veyron2.services.store.TransactionID TID, java.util.ArrayList<java.lang.Object> Options) throws com.veyron2.ipc.VeyronException {
		createTransaction(context, TID, Options, null);
	}
	@Override
	public void createTransaction(com.veyron2.ipc.Context context, com.veyron2.services.store.TransactionID TID, java.util.ArrayList<java.lang.Object> Options, com.veyron2.Options veyronOpts) throws com.veyron2.ipc.VeyronException {
		// Prepare input arguments.
		final java.lang.Object[] inArgs = new java.lang.Object[]{ TID, Options };

		// Add VDL path option.
		// NOTE(spetrovic): this option is temporary and will be removed soon after we switch
		// Java to encoding/decoding from vom.Value objects.
		if (veyronOpts == null) veyronOpts = new com.veyron2.Options();
		if (!veyronOpts.has(com.veyron2.OptionDefs.VDL_INTERFACE_PATH)) {
			veyronOpts.set(com.veyron2.OptionDefs.VDL_INTERFACE_PATH, StoreStub.vdlIfacePathOpt);
		}

		// Start the call.
		final com.veyron2.ipc.Client.Call call = this.client.startCall(context, this.name, "CreateTransaction", inArgs, veyronOpts);

		// Prepare output argument and finish the call.
			final com.google.common.reflect.TypeToken<?>[] resultTypes = new com.google.common.reflect.TypeToken<?>[]{  };
			call.finish(resultTypes);

	}
	@Override
	public void commit(com.veyron2.ipc.Context context, com.veyron2.services.store.TransactionID TID) throws com.veyron2.ipc.VeyronException {
		commit(context, TID, null);
	}
	@Override
	public void commit(com.veyron2.ipc.Context context, com.veyron2.services.store.TransactionID TID, com.veyron2.Options veyronOpts) throws com.veyron2.ipc.VeyronException {
		// Prepare input arguments.
		final java.lang.Object[] inArgs = new java.lang.Object[]{ TID };

		// Add VDL path option.
		// NOTE(spetrovic): this option is temporary and will be removed soon after we switch
		// Java to encoding/decoding from vom.Value objects.
		if (veyronOpts == null) veyronOpts = new com.veyron2.Options();
		if (!veyronOpts.has(com.veyron2.OptionDefs.VDL_INTERFACE_PATH)) {
			veyronOpts.set(com.veyron2.OptionDefs.VDL_INTERFACE_PATH, StoreStub.vdlIfacePathOpt);
		}

		// Start the call.
		final com.veyron2.ipc.Client.Call call = this.client.startCall(context, this.name, "Commit", inArgs, veyronOpts);

		// Prepare output argument and finish the call.
			final com.google.common.reflect.TypeToken<?>[] resultTypes = new com.google.common.reflect.TypeToken<?>[]{  };
			call.finish(resultTypes);

	}
	@Override
	public void abort(com.veyron2.ipc.Context context, com.veyron2.services.store.TransactionID TID) throws com.veyron2.ipc.VeyronException {
		abort(context, TID, null);
	}
	@Override
	public void abort(com.veyron2.ipc.Context context, com.veyron2.services.store.TransactionID TID, com.veyron2.Options veyronOpts) throws com.veyron2.ipc.VeyronException {
		// Prepare input arguments.
		final java.lang.Object[] inArgs = new java.lang.Object[]{ TID };

		// Add VDL path option.
		// NOTE(spetrovic): this option is temporary and will be removed soon after we switch
		// Java to encoding/decoding from vom.Value objects.
		if (veyronOpts == null) veyronOpts = new com.veyron2.Options();
		if (!veyronOpts.has(com.veyron2.OptionDefs.VDL_INTERFACE_PATH)) {
			veyronOpts.set(com.veyron2.OptionDefs.VDL_INTERFACE_PATH, StoreStub.vdlIfacePathOpt);
		}

		// Start the call.
		final com.veyron2.ipc.Client.Call call = this.client.startCall(context, this.name, "Abort", inArgs, veyronOpts);

		// Prepare output argument and finish the call.
			final com.google.common.reflect.TypeToken<?>[] resultTypes = new com.google.common.reflect.TypeToken<?>[]{  };
			call.finish(resultTypes);

	}
	@Override
	public com.veyron2.vdl.ClientStream<java.lang.Void,Conflict,java.lang.Void> readConflicts(com.veyron2.ipc.Context context) throws com.veyron2.ipc.VeyronException {
		return readConflicts(context, null);
	}
	@Override
	public com.veyron2.vdl.ClientStream<java.lang.Void,Conflict,java.lang.Void> readConflicts(com.veyron2.ipc.Context context, com.veyron2.Options veyronOpts) throws com.veyron2.ipc.VeyronException {
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
		final com.veyron2.ipc.Client.Call call = this.client.startCall(context, this.name, "ReadConflicts", inArgs, veyronOpts);

		return new com.veyron2.vdl.ClientStream<java.lang.Void, Conflict, java.lang.Void>() {
			@Override
			public void send(java.lang.Void item) throws com.veyron2.ipc.VeyronException {
				call.send(item);
			}
			@Override
			public Conflict recv() throws java.io.EOFException, com.veyron2.ipc.VeyronException {
				final com.google.common.reflect.TypeToken<?> type = new com.google.common.reflect.TypeToken<Conflict>() {};
				final java.lang.Object result = call.recv(type);
				try {
					return (Conflict)result;
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