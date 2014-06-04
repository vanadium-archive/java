package com.veyron2.ipc;

import com.google.common.reflect.TypeToken;
import com.veyron2.ipc.Stream;

/**
 * Client represents the interface for making RPC calls.  There may be multiple
 * outstanding Client.Calls associated with a single Client, and a Client may be
 * used by multiple goroutines concurrently.
 */
public interface Client {
	/**
	 * Starts an asynchronous call of the method on the server instance
	 * identified by name, with the given input args (of any arity).  The returned
	 * Call object manages streaming args and results, and finishes the call.
	 *
	 * @param  name            a name of the server
	 * @param  method          a name of the server's method to be invoked
	 * @param  args            an array of arguments to the server's method
	 * @param  opts            call options
	 * @return Call            a call object that manages streaming args and results
	 * @throws VeyronException if the call cannot be started
	 */
	public Call startCall(Context context, String name, String method, Object[] args, CallOption... opts) throws VeyronException;

	/**
	 * Discards all state associated with this Client.  In-flight calls may
	 * be terminated with an error.
	 */
	public void close();

	/**
	 * Call defines the interface for each in-flight call on the client.
	 * Method finish() must be called to finish the call; all other methods are optional.
	 */
	public interface Call extends Stream {
		/**
		 * Indicates to the server that no more items will be sent; server
		 * Recv calls will throw EOFException after all sent items.  Subsequent calls to
		 * Send on the client will fail.  This is an optional call - it's used by
		 * streaming clients that need the server to throw EOFException.
		 *
		 * @throws VeyronException if there was an error closing.
		 */
		public void closeSend() throws VeyronException;

		/**
		 * Blocks until the server has finished the call and returns
		 * the positional output arguments (of any arity).
		 *
		 * @param  types           types for all the output arguments
		 * @return Object[]        an array of output arguments
		 * @throws VeyronException if there was an error executing the call
		 */
		public Object[] finish(TypeToken<?>[] types) throws VeyronException;

		/**
		 * Cancels the call.  The server will stop processing, if possible.  Calls to
		 * {@link #finish} will return immediately with an error indicating the cancellation.
		 * It is safe to call Cancel concurrently with any other ClientCall method.
		 */
		public void cancel();
	}

	/**
	 * CallOption represents an option that can be passed to Client's {@link #startCall} method.
	 */
	public interface CallOption {}

	/**
	 * BindOption represents an option that can be passed to IDL Client's Bind* methods.
	 */
	public interface BindOption {}
}