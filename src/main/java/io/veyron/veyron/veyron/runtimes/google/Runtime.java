package io.veyron.veyron.veyron.runtimes.google;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import org.joda.time.DateTime;
import org.joda.time.Duration;

import io.veyron.veyron.veyron.runtimes.google.android.RedirectStderr;
import io.veyron.veyron.veyron.runtimes.google.naming.Namespace;
import io.veyron.veyron.veyron.runtimes.google.security.PrivateID;
import io.veyron.veyron.veyron.runtimes.google.security.PublicIDStore;
import io.veyron.veyron.veyron.runtimes.google.security.Signer;
import io.veyron.veyron.veyron.runtimes.google.security.Util;
import io.veyron.veyron.veyron2.OptionDefs;
import io.veyron.veyron.veyron2.Options;
import io.veyron.veyron.veyron2.context.CancelableContext;
import io.veyron.veyron.veyron2.context.Context;
import io.veyron.veyron.veyron2.ipc.Dispatcher;
import io.veyron.veyron.veyron2.ipc.VeyronException;
import io.veyron.veyron.veyron2.security.Blessings;
import io.veyron.veyron.veyron2.security.CryptoUtil;
import io.veyron.veyron.veyron2.security.Label;
import io.veyron.veyron.veyron2.security.Principal;
import io.veyron.veyron.veyron2.security.PublicID;
import io.veyron.veyron.veyron2.security.Security;
import io.veyron.veyron.veyron2.security.SecurityConstants;
import io.veyron.veyron.veyron2.security.wire.ChainPublicID;
import io.veyron.veyron.veyron2.vdl.Any;
import io.veyron.veyron.veyron2.vdl.JSONUtil;

import java.io.EOFException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.interfaces.ECPublicKey;
import java.util.concurrent.CountDownLatch;

/**
 * Runtime is an implementation of Veyron Runtime that calls to native Go code for most of its
 * functionalities.
 */
public class Runtime implements io.veyron.veyron.veyron2.Runtime {
	private static final String TAG = "Veyron runtime";
	private static Runtime globalRuntime = null;

	private static native long nativeInit(Options opts) throws VeyronException;
	private static native long nativeNewRuntime(Options opts) throws VeyronException;

	/**
	 * Returns the initialized global instance of the Runtime.
	 *
	 * @param  ctx  android context.
	 * @param  opts runtime options.
	 * @return      a pre-initialized runtime instance.
	 */
	public static synchronized Runtime init(android.content.Context ctx, Options opts) {
		if (Runtime.globalRuntime == null) {
			if (opts == null) {
				opts = new Options();
			}
			try {
				setupRuntimeOptions(ctx, opts);
				final Principal principal = (Principal)opts.get(OptionDefs.RUNTIME_PRINCIPAL);
				final io.veyron.veyron.veyron2.security.PrivateID privateID =
					(io.veyron.veyron.veyron2.security.PrivateID)opts.get(OptionDefs.RUNTIME_ID);
				Runtime.globalRuntime = new Runtime(nativeInit(opts), principal, privateID);
			} catch (VeyronException e) {
				throw new RuntimeException(
					"Couldn't initialize global Veyron Runtime instance: " + e.getMessage());
			}
		}
		return Runtime.globalRuntime;
	}

	/**
	 * Returns the pre-initialized global Runtime instance.  Returns <code>null</code> if init()
	 * hasn't already been invoked.
	 *
	 * @return a pre-initialized runtime instance.
	 */
	public static synchronized Runtime defaultRuntime() {
		return Runtime.globalRuntime;
	}

	/**
	 * Creates and initializes a new Runtime instance.
	 *
	 * @param  ctx  android context.
	 * @param  opts runtime options.
	 * @return      a new runtime instance.
	 */
	public static synchronized Runtime newRuntime(android.content.Context ctx, Options opts) {
		if (opts == null) {
			opts = new Options();
		}
		try {
			setupRuntimeOptions(ctx, opts);
			final Principal principal = (Principal)opts.get(OptionDefs.RUNTIME_PRINCIPAL);
			final io.veyron.veyron.veyron2.security.PrivateID privateID =
				(io.veyron.veyron.veyron2.security.PrivateID)opts.get(OptionDefs.RUNTIME_ID);
			return new Runtime(nativeNewRuntime(opts), principal, privateID);
		} catch (VeyronException e) {
			throw new RuntimeException("Couldn't create Veyron Runtime: " + e.getMessage());
		}
	}

	private static void setupRuntimeOptions(android.content.Context ctx, Options opts)
		throws VeyronException {
		// If the PrivateID option isn't specified, generate a new PrivateID.  Note that we
		// choose to generate keys inside Java (instead of native code) because we can
		// conveniently store them inside Android KeyStore.
		if (!opts.has(OptionDefs.RUNTIME_ID) || opts.get(OptionDefs.RUNTIME_ID) == null) {
			// Check if the private key has already been generated for this package.
			// (NOTE: Android package names are unique.)
			KeyStore.PrivateKeyEntry keyEntry =
				CryptoUtil.getKeyStorePrivateKey(ctx.getPackageName());
			if (keyEntry == null) {
				// Generate a new private key.
				keyEntry = CryptoUtil.genKeyStorePrivateKey(ctx, ctx.getPackageName());
			}
			final Signer signer = new Signer(
				keyEntry.getPrivateKey(), (ECPublicKey)keyEntry.getCertificate().getPublicKey());
			final PrivateID id = PrivateID.create(ctx.getPackageName(), signer);
			opts.set(OptionDefs.RUNTIME_ID, id);
		}

		if (!opts.has(OptionDefs.RUNTIME_PRINCIPAL) ||
			opts.get(OptionDefs.RUNTIME_PRINCIPAL) == null) {
			// Check if the private key has already been generated for this package.
			// (NOTE: Android package names are unique.)
			KeyStore.PrivateKeyEntry keyEntry =
				CryptoUtil.getKeyStorePrivateKey(ctx.getPackageName());
			if (keyEntry == null) {
				// Generate a new private key.
				keyEntry = CryptoUtil.genKeyStorePrivateKey(ctx, ctx.getPackageName());
			}
			final Signer signer = new Signer(
				keyEntry.getPrivateKey(), (ECPublicKey)keyEntry.getCertificate().getPublicKey());
			final Principal principal = createPrincipal(ctx, signer);
			opts.set(OptionDefs.RUNTIME_PRINCIPAL, principal);
		}
	}

	private static Principal createPrincipal(android.content.Context ctx, Signer signer)
		throws VeyronException {
		final Principal principal = Security.newPrincipal(signer);
		final Blessings blessings = principal.blessSelf(ctx.getPackageName());
		principal.blessingStore().setDefaultBlessings(blessings);
		principal.blessingStore().set(blessings, SecurityConstants.ALL_PRINCIPALS);
		principal.addToRoots(blessings);
		return principal;
	}

	private static void encodeLocalIDOption(Options opts) throws VeyronException {
		if (opts == null || !opts.has(OptionDefs.LOCAL_ID)) {
			return;
		}
		// Encode the id.
		final PublicID id = opts.get(OptionDefs.LOCAL_ID, PublicID.class);
		if (id == null) {
			opts.remove(OptionDefs.LOCAL_ID);
			return;
		}
		final ChainPublicID[] chains = id.encode();
		final String[] encodedChains = Util.encodeChains(chains);
		opts.set(OptionDefs.LOCAL_ID, encodedChains);
	}

	static {
		System.loadLibrary("jniwrapper");
		System.loadLibrary("veyronjni");
		RedirectStderr.Start();
	}

	private final long nativePtr;
	private Client client;
	private final Principal principal;  // non-null
	private final io.veyron.veyron.veyron2.security.PrivateID privateID;  // non-null.
	private io.veyron.veyron.veyron2.security.PublicIDStore publicIDStore;

	private native Client nativeNewClient(long nativePtr, Options opts) throws VeyronException;
	private native Server nativeNewServer(long nativePtr, Options opts) throws VeyronException;
	private native Client nativeGetClient(long nativePtr) throws VeyronException;
	private native Context nativeNewContext(long nativePtr) throws VeyronException;
	private native long nativeGetPublicIDStore(long nativePtr);
	private native long nativeGetNamespace(long nativePtr);
	private native void nativeFinalize(long nativePtr);

	private Runtime(long nativePtr, Principal principal,
		io.veyron.veyron.veyron2.security.PrivateID privateID) {
		this.nativePtr = nativePtr;
		this.principal = principal;
		this.privateID = privateID;
	}
	@Override
	public io.veyron.veyron.veyron2.ipc.Client newClient() throws VeyronException {
		return newClient(null);
	}
	@Override
	public io.veyron.veyron.veyron2.ipc.Client newClient(Options opts) throws VeyronException {
		encodeLocalIDOption(opts);
		return nativeNewClient(this.nativePtr, opts);
	}
	@Override
	public io.veyron.veyron.veyron2.ipc.Server newServer() throws VeyronException {
		return newServer(null);
	}
	@Override
	public io.veyron.veyron.veyron2.ipc.Server newServer(Options opts) throws VeyronException {
		encodeLocalIDOption(opts);
		return nativeNewServer(this.nativePtr, opts);
	}
	@Override
	public synchronized io.veyron.veyron.veyron2.ipc.Client getClient() {
		if (this.client == null) {
			try {
				this.client = nativeGetClient(this.nativePtr);
			} catch (VeyronException e) {
				android.util.Log.e(TAG, "Coudln't get client: " + e.getMessage());
				return null;
			}
		}
		return this.client;
	}
	@Override
	public Context newContext() {
		try {
			return nativeNewContext(this.nativePtr);
		} catch (VeyronException e) {
			android.util.Log.e(TAG, "Couldn't get new context: " + e.getMessage());
			return null;
		}
	}
	@Override
	public Principal getPrincipal() {
		return this.principal;
	}
	@Override
	public io.veyron.veyron.veyron2.security.PrivateID newIdentity(String name) throws VeyronException {
		try {
			// Generate a new private key, stored in the clear in the app's memory.
			final KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
			final SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
			keyGen.initialize(256, random);
			final KeyPair keyPair = keyGen.generateKeyPair();
			final Signer signer = new Signer(keyPair.getPrivate(), (ECPublicKey)keyPair.getPublic());
			return PrivateID.create(name, signer);
		} catch (NoSuchAlgorithmException e) {
			throw new VeyronException("ECDSA algorithm not supported: " + e.getMessage());
		}
	}
	@Override
	public io.veyron.veyron.veyron2.security.PrivateID getIdentity() {
		return this.privateID;
	}
	@Override
	public io.veyron.veyron.veyron2.security.PublicIDStore getPublicIDStore() {
		if (this.publicIDStore == null) {
			this.publicIDStore = new PublicIDStore(nativeGetPublicIDStore(this.nativePtr));
		}
		return this.publicIDStore;
	}
	@Override
	public io.veyron.veyron.veyron2.naming.Namespace getNamespace() {
		return new Namespace(nativeGetNamespace(this.nativePtr));
	}
	@Override
	protected void finalize() {
		nativeFinalize(this.nativePtr);
	}

	private static class Server implements io.veyron.veyron.veyron2.ipc.Server {
		private final long nativePtr;

		private native String nativeListen(long nativePtr, String protocol, String address)
			throws VeyronException;
		private native void nativeServe(long nativePtr, String name, Dispatcher dispatcher)
			throws VeyronException;
		private native String[] nativeGetPublishedNames(long nativePtr) throws VeyronException;
		private native void nativeStop(long nativePtr) throws VeyronException;
		private native void nativeFinalize(long nativePtr);

		private Server(long nativePtr) {
			this.nativePtr = nativePtr;
		}
		// Implement io.veyron.veyron.veyron2.ipc.Server.
		@Override
		public String listen(String protocol, String address) throws VeyronException {
			return nativeListen(this.nativePtr, protocol, address);
		}
		@Override
		public void serve(String name, Dispatcher dispatcher) throws VeyronException {
			nativeServe(this.nativePtr, name, dispatcher);
		}
		@Override
		public String[] getPublishedNames() throws VeyronException {
			return nativeGetPublishedNames(this.nativePtr);
		}
		@Override
		public void stop() throws VeyronException {
			nativeStop(this.nativePtr);
		}
		// Implement java.lang.Object.
		@Override
		protected void finalize() {
			nativeFinalize(this.nativePtr);
		}
	}

	private static class Client implements io.veyron.veyron.veyron2.ipc.Client {
		private final long nativePtr;
		private final Gson gson;

		private native ClientCall nativeStartCall(long nativePtr, Context context, String name,
			String method, String[] args, Options opts) throws VeyronException;
		private native void nativeClose(long nativePtr);
		private native void nativeFinalize(long nativePtr);

		private Client(long nativePtr) {
			this.nativePtr = nativePtr;
			this.gson = JSONUtil.getGsonBuilder().create();
		}
		// Implement io.veyron.veyron.veyron2.ipc.Client.
		@Override
		public Call startCall(Context context, String name, String method, Object[] args)
			throws VeyronException {
			return startCall(context, name, method, args, null);
		}
		@Override
		public Call startCall(Context context, String name, String method, Object[] args,
			Options opts) throws VeyronException {
			if (method == "") {
				throw new VeyronException("Empty method name invoked on object %s", name);
			}

			// Encode all input arguments to JSON.
			final String[] jsonArgs = new String[args.length];
			for (int i = 0; i < args.length; i++) {
				jsonArgs[i] = this.gson.toJson(args[i]);
			}

			// Invoke native method.
			// Make sure that the method name starts with an uppercase character.
			method = Character.toUpperCase(method.charAt(0)) + method.substring(1);
			return nativeStartCall(this.nativePtr, context, name, method, jsonArgs, opts);
		}
		@Override
		public void close() {
			nativeClose(this.nativePtr);
		}
		// Implement java.lang.Object.
		@Override
		protected void finalize() {
			nativeFinalize(this.nativePtr);
		}
	}

	private static class Stream implements io.veyron.veyron.veyron2.ipc.Stream {
		private final long nativePtr;
		private final Gson gson;

		private native void nativeSend(long nativePtr, String item) throws VeyronException;
		private native String nativeRecv(long nativePtr) throws EOFException, VeyronException;
		private native void nativeFinalize(long nativePtr);

		private Stream(long nativePtr) {
			this.nativePtr = nativePtr;
			this.gson = JSONUtil.getGsonBuilder().create();
		}
		@Override
		public void send(Object item) throws VeyronException {
			nativeSend(nativePtr, this.gson.toJson(item));
		}

		@Override
		public Object recv(TypeToken<?> type) throws EOFException, VeyronException {
			final String result = nativeRecv(nativePtr);
			try {
				return this.gson.fromJson(result, type.getType());
			} catch (JsonSyntaxException e) {
				throw new VeyronException(String.format(
					"Error decoding result %s from JSON: %s", result, e.getMessage()));
			}
		}
		@Override
		protected void finalize() {
			nativeFinalize(this.nativePtr);
		}
	}

	private static class ClientCall implements io.veyron.veyron.veyron2.ipc.Client.Call {
		private final long nativePtr;
		private final Stream stream;
		private final Gson gson;

		private native String[] nativeFinish(long nativePtr) throws VeyronException;
		private native void nativeCancel(long nativePtr);
		private native void nativeFinalize(long nativePtr);

		private ClientCall(long nativePtr, Stream stream) {
			this.nativePtr = nativePtr;
			this.stream = stream;
			this.gson = JSONUtil.getGsonBuilder().create();
		}

		// Implements io.veyron.veyron.veyron2.ipc.Client.Call.
		@Override
		public void closeSend() throws VeyronException {
			// TODO(spetrovic): implement this.
		}
		@Override
		public Object[] finish(TypeToken<?>[] types) throws VeyronException {
			// Call native method.
			final String[] jsonResults = nativeFinish(this.nativePtr);
			if (jsonResults.length != types.length) {
				throw new VeyronException(String.format(
					"Mismatch in number of results, want %s, have %s",
					types.length, jsonResults.length));
			}

			// JSON-decode results and return.
			final Object[] ret = new Object[types.length];
			for (int i = 0; i < types.length; i++) {
				final TypeToken<?> type = types[i];
				final String jsonResult = jsonResults[i];
				if (type.equals(new TypeToken<Any>(){})) {  // Any type.
					ret[i] = new Any(jsonResult);
					continue;
				}
				try {
					ret[i] = this.gson.fromJson(jsonResult, type.getType());
				} catch (JsonSyntaxException e) {
					throw new VeyronException(String.format(
						"Error decoding JSON result %s into type %s: %s",
						jsonResult, e.getMessage()));
				}
			}
			return ret;
		}
		@Override
		public void cancel() {
			nativeCancel(this.nativePtr);
		}
		// Implements io.veyron.veyron.veyron2.ipc.Stream.
		@Override
		public void send(Object item) throws VeyronException {
			this.stream.send(item);
		}
		@Override
		public Object recv(TypeToken<?> type) throws EOFException, VeyronException {
			return this.stream.recv(type);
		}
		// Implements java.lang.Object.
		@Override
		protected void finalize() {
			nativeFinalize(this.nativePtr);
		}
	}

	@SuppressWarnings("unused")
	private static class ServerCall implements io.veyron.veyron.veyron2.ipc.ServerCall {
		private final long nativePtr;
		private final Stream stream;
		private final Context context;
		private final io.veyron.veyron.veyron.runtimes.google.security.Context securityContext;

		public native Blessings nativeBlessings(long nativePtr) throws VeyronException;
		private native void nativeFinalize(long nativePtr);

		private ServerCall(long nativePtr, Stream stream, Context context,
			io.veyron.veyron.veyron.runtimes.google.security.Context securityContext) {
			this.nativePtr = nativePtr;
			this.stream = stream;
			this.context = context;
			this.securityContext = securityContext;
		}
		// Implements io.veyron.veyron.veyron2.ipc.ServerContext.
		@Override
		public Blessings blessings() {
			try {
				return nativeBlessings(this.nativePtr);
			} catch (VeyronException e) {
				android.util.Log.e(TAG, "Couldn't get blessings: " + e.getMessage());
				return null;
			}
		}
		// Implements io.veyron.veyron.veyron2.ipc.Stream.
		@Override
		public void send(Object item) throws VeyronException {
			this.stream.send(item);
		}
		@Override
		public Object recv(TypeToken<?> type) throws EOFException, VeyronException {
			return this.stream.recv(type);
		}
		// Implements io.veyron.veyron.veyron2.context.Context.
		@Override
		public DateTime deadline() {
			return this.context.deadline();
		}
		@Override
		public CountDownLatch done() {
			return this.context.done();
		}
		@Override
		public Object value(Object key) {
			return this.context.value(key);
		}
		@Override
		public CancelableContext withCancel() {
			return this.context.withCancel();
		}
		@Override
		public CancelableContext withDeadline(DateTime deadline) {
			return this.context.withDeadline(deadline);
		}
		@Override
		public CancelableContext withTimeout(Duration timeout) {
			return this.context.withTimeout(timeout);
		}
		@Override
		public Context withValue(Object key, Object value) {
			return this.context.withValue(key, value);
		}
		// Implements io.veyron.veyron.veyron2.security.Context.
		@Override
		public String method() {
			return this.securityContext.method();
		}
		@Override
		public String name() {
			return this.securityContext.name();
		}
		@Override
		public String suffix() {
			return this.securityContext.suffix();
		}
		@Override
		public Label label() {
			return this.securityContext.label();
		}
		@Override
		public String localEndpoint() {
			return this.securityContext.localEndpoint();
		}
		@Override
		public String remoteEndpoint() {
			return this.securityContext.remoteEndpoint();
		}
		@Override
		public Principal localPrincipal() {
			return this.securityContext.localPrincipal();
		}
		@Override
		public Blessings localBlessings() {
			return this.securityContext.localBlessings();
		}
		@Override
		public Blessings remoteBlessings() {
			return this.securityContext.remoteBlessings();
		}
		@Override
		public PublicID localID() {
			return this.securityContext.localID();
		}
		@Override
		public PublicID remoteID() {
			return this.securityContext.remoteID();
		}
		// Implements java.lang.Object.
		@Override
		protected void finalize() {
			nativeFinalize(this.nativePtr);
		}
	}
}