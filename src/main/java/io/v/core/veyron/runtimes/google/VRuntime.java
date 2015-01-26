package io.v.core.veyron.runtimes.google;

import io.v.core.veyron2.OptionDefs;
import io.v.core.veyron2.Options;
import io.v.core.veyron2.VeyronException;
import io.v.core.veyron2.context.VContext;
import io.v.core.veyron2.ipc.ListenSpec;
import io.v.core.veyron2.naming.Namespace;
import io.v.core.veyron2.security.Principal;

/**
 * VRuntime is an implementation of {@code io.v.core.veyron2.VRuntime} that calls to native Go
 * code for most of its functionalities.
 */
public class VRuntime implements io.v.core.veyron2.VRuntime {
	private static final String TAG = "Veyron runtime";

	private static native VContext nativeInit() throws VeyronException;

	/**
	 * Returns a new runtime instance.
	 *
	 * @param  opts runtime options
	 * @return      a new runtime instance
	 */
	public static VRuntime create(Options opts) throws VeyronException {
		final ListenSpec listenSpec = (ListenSpec) opts.get(OptionDefs.DEFAULT_LISTEN_SPEC);
		return new VRuntime(nativeInit(), listenSpec);
	}

	private final VContext ctx;  // non-null
	private final ListenSpec listenSpec;

	private native VContext nativeSetNewClient(VContext ctx, Options opts) throws VeyronException;
	private native io.v.core.veyron2.ipc.Client nativeGetClient(VContext ctx)
			throws VeyronException;
	private native io.v.core.veyron2.ipc.Server nativeNewServer(VContext ctx, Options opts)
			throws VeyronException;
	private native VContext nativeSetPrincipal(VContext ctx, Principal principal)
			throws VeyronException;
	private native Principal nativeGetPrincipal(VContext ctx) throws VeyronException;
	private native VContext nativeSetNewNamespace(VContext ctx, String... roots)
			throws VeyronException;
	private native Namespace nativeGetNamespace(VContext ctx) throws VeyronException;

	private VRuntime(VContext ctx, ListenSpec listenSpec) {
		this.ctx = ctx;
		this.listenSpec = listenSpec;
	}
	@Override
	public VContext setNewClient(VContext ctx, Options opts) throws VeyronException {
		return nativeSetNewClient(ctx, opts);
	}
	@Override
	public io.v.core.veyron2.ipc.Client getClient(VContext ctx) {
		try {
			return nativeGetClient(ctx);
		} catch (VeyronException e) {
			throw new RuntimeException("Couldn't get client: " + e.getMessage());
		}
	}
	@Override
	public io.v.core.veyron2.ipc.Server newServer(VContext ctx, Options opts)
		throws VeyronException {
		if (this.listenSpec != null && opts.get(OptionDefs.DEFAULT_LISTEN_SPEC) == null) {
			opts.set(OptionDefs.DEFAULT_LISTEN_SPEC, this.listenSpec);
		}
		return nativeNewServer(ctx, opts);
	}
	@Override
	public VContext setPrincipal(VContext ctx, Principal principal) throws VeyronException {
		return nativeSetPrincipal(ctx, principal);
	}
	@Override
	public Principal getPrincipal(VContext ctx) {
		try {
			return nativeGetPrincipal(ctx);
		} catch (VeyronException e) {
			throw new RuntimeException("Couldn't get principal: " + e.getMessage());
		}
	}
	@Override
	public VContext setNewNamespace(VContext ctx, String... roots) throws VeyronException {
		return nativeSetNewNamespace(ctx, roots);
	}
	@Override
	public Namespace getNamespace(VContext ctx) {
		try {
			return nativeGetNamespace(ctx);
		} catch (VeyronException e) {
			throw new RuntimeException("Couldn't get namespace: " + e.getMessage());
		}
	}
	@Override
	public VContext getContext() {
		return this.ctx;
	}
}