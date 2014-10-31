package io.veyron.veyron.veyron2.ipc;

import com.google.common.reflect.TypeToken;

import io.veyron.veyron.veyron2.Options;
import io.veyron.veyron.veyron2.context.Context;

/**
 * Client represents the interface for making RPC calls.  There may be multiple outstanding calls
 * associated with a single client, and a client may be used by multiple threads concurrently.
 */
public interface Client {
	/**
	 * Starts an asynchronous call of the method on the server instance identified by name,
	 * with the given input args (of any arity).  The returned call object manages streaming args
	 * and results and finishes the call.
	 *
	 * @param  context         client context.
	 * @param  name            name of the server.
	 * @param  method          name of the server's method to be invoked.
	 * @param  args            array of arguments to the server's method.
	 * @return                 call object that manages streaming args and results.
	 * @throws VeyronException if the call cannot be started.
	 */
	public Call startCall(Context context, String name, String method, Object[] args)
		throws VeyronException;

	/**
	 * Starts an asynchronous call of the method on the server instance identified by name, with the
	 * given input args (of any arity) and provided options.  The returned call object manages
	 * streaming args and results and finishes the call.
	 * A particular implementation of this interface chooses which options to support,
	 * but at the minimum it must handle the following pre-defined options:
	 *    CURRENTLY NO OPTIONS ARE MANDATED
	 *
	 * @param  context         client context.
	 * @param  name            a name of the server.
	 * @param  method          a name of the server's method to be invoked.
	 * @param  args            an array of arguments to the server's method.
	 * @param  opts            call options.
	 * @return                 a call object that manages streaming args and results.
	 * @throws VeyronException if the call cannot be started.
	 */
	public Call startCall(Context context, String name, String method, Object[] args,
		Options opts) throws VeyronException;

	/**
	 * Discards all the state associated with this client.  In-flight calls may be terminated with
	 * an error.
	 */
	public void close();

	/**
	 * Call defines the interface for each in-flight call on the client.  Method {@code finish()}
	 * must be called to finish the call; all other methods are optional.
	 */
	public interface Call extends Stream {
		/**
		 * Indicates to the server that no more items will be sent; server's {@code recv} calls will
		 * throw {@code EOFException} after all sent items.  Subsequent calls to {@code send} on the
		 * client will fail.  This is an optional call - it's used by streaming clients that need
		 * the server to throw {@code EOFException}.
		 *
		 * @throws VeyronException if there was an error closing.
		 */
		public void closeSend() throws VeyronException;

		/**
		 * Blocks until the server has finished the call and returns the positional output arguments
		 * (of any arity).
		 *
		 * @param  types           types for all the output arguments.
		 * @return                 an array of output arguments.
		 * @throws VeyronException if there was an error executing the call.
		 */
		public Object[] finish(TypeToken<?>[] types) throws VeyronException;

		/**
		 * Cancels the call.  The server will stop processing, if possible.  Calls to
		 * {@code finish()} will return immediately with an error indicating the cancellation.
		 * It is safe to call {@code cancel()} concurrently with any other call method.
		 */
		public void cancel();
	}
}