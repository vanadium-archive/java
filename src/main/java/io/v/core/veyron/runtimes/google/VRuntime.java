package io.v.core.veyron.runtimes.google;

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
	private static final ListenSpec DEFAULT_LISTEN_SPEC = new ListenSpec(
			new ListenSpec.Address[] { new ListenSpec.Address("tcp", ":0") },
			"/ns.dev.v.io:8101/proxy",
			false);

	private static native VContext nativeInit() throws VeyronException;
	private static native VContext nativeSetNewClient(VContext ctx, Options opts)
			throws VeyronException;
	private static native io.v.core.veyron2.ipc.Client nativeGetClient(VContext ctx)
			throws VeyronException;
	private static native io.v.core.veyron2.ipc.Server nativeNewServer(
			VContext ctx, ListenSpec spec) throws VeyronException;
	private static native VContext nativeSetPrincipal(VContext ctx, Principal principal)
			throws VeyronException;
	private static native Principal nativeGetPrincipal(VContext ctx) throws VeyronException;
	private static native VContext nativeSetNewNamespace(VContext ctx, String... roots)
			throws VeyronException;
	private static native Namespace nativeGetNamespace(VContext ctx) throws VeyronException;

	/**
	 * Returns a new runtime instance.
	 *
	 * @return      a new runtime instance
	 */
	public static VRuntime create() throws VeyronException {
		return new VRuntime(nativeInit());
	}

	private final VContext ctx;  // non-null

	private VRuntime(VContext ctx) {
		this.ctx = setListenSpec(ctx, DEFAULT_LISTEN_SPEC);
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
		// Get a Java ListenSpec is attached to this context.
		final ListenSpec spec = (ListenSpec) ctx.value(this);
		if (spec == null) {
			throw new VeyronException("Couldn't get attached listen spec");
		}
		return nativeNewServer(ctx, spec);
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
	public VContext setListenSpec(VContext ctx, ListenSpec spec) {
		return ctx.withValue(this, spec);
	}

	@Override
	public ListenSpec getListenSpec(VContext ctx) {
		// Get the ListenSpec attached to this context.
		final ListenSpec spec = (ListenSpec) ctx.value(this);
		if (spec == null) {
			throw new RuntimeException("Couldn't get attached listen spec");
		}
		return spec;
	}
	@Override
	public VContext getContext() {
		return this.ctx;
	}
}