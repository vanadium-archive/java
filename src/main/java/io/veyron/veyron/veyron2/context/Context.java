package io.veyron.veyron.veyron2.context;

import org.joda.time.DateTime;
import org.joda.time.Duration;

import java.util.concurrent.CountDownLatch;

/**
 * Context is an interface for carrying deadlines, cancellation, and data across API
 * boundaries.
 *
 * Server method implementations receive a Context as their first argument and should generally
 * pass this Context (or a derivitive) on to dependant operations.  You should allocate new
 * Context objects only for operations that are semantically unrelated to any ongoing calls.
 *
 * New Contexts are created via the runtime with <code>Runtime.newContext()</code>.
 * New Contexts represent a fresh operation that's unrelated to any other ongoing work.  Further
 * Contexts can be derived from the original Context to refine the environment.  For example if part
 * of your operation requires a finer deadline you can create a new Context to handle that part:
 *
 *    final Context ctx = runtime.newContext();
 *    // We'll use cacheCtx to lookup data in memcache; if it takes more than a
 *    // second to get data from memcache we should just skip the cache and
 *    // perform the slow operation.
 *    final Context cacheCtx = ctx.withTimeout(Duration.standardSeconds(1));
 *    try {
 *    	fetchDataFromMemcache(cacheCtx, key);
 *    } catch (VeyronException e) {
 *      if (e.equals(DEADLINE_EXCEEDED)) {
 *        recomputeData(ctx, key);
 *      }
 *    }
 *
 * Contexts form a tree where derived Contexts are children of the Contexts from which they were
 * derived.  Children inherit all the properties of their parent except for the property being
 * replaced (the deadline in the example above).
 *
 * Contexts are extensible.  The <code>value</code>/<code>withValue<code> methods allow you to
 * attach new information to the Context and extend its capabilities. In the same way we derive
 * new Contexts via the 'with' family of functions you can create methods to attach new data:
 *
 *    public class Auth {
 *        private class AuthKey {}
 *
 *        private static final AuthKey KEY = new AuthKey();
 *
 *        public Context withAuth(Context parent, Auth data) {
 *          return parent.withValue(KEY, data);
 *        }
 *
 *        public Auth fromContext(Context ctx) {
 *        	return (Auth)ctx.Value(KEY);
 *        }
 *    }
 *
 * Note that any type can be used as a key but the caller should preferrably use a private or
 * protected type to prevent collisions (i.e., to prevent somebody else instantiating a key of the
 * same type).  Keys are tested for equality by comparing their value pairs
 * <code>(getClass(), hashCode())</code>.
 */
public interface Context {
	/**
	 * Returns the time at which this Context will be automatically canceled, or <code>null</code>
	 * if this Context doesn't have a deadline.
	 *
	 * @return the time at which this Context will be automatically canceled or <code>null</code>
	 *         if there is no deadline.
	 */
	public DateTime deadline();

	/**
	 * Returns a counter that will reach the count of zero when this Context is canceled (either
	 * explicitly or via an expired deadline).  Callers may block on this counter or periodically
	 * poll it.  When the counter reaches value zero, any blocked threads are released.
	 *
	 * Successive calls to this method must always return the same value.  Implementations may
	 * return <code>null</code> if they can never be canceled.
	 *
	 * @return a counter that reaches the count of zero when this Context is canceled or exceeds
	 *         its deadline, or <code>null</code> if the counter can never be canceled.
	 */
	public CountDownLatch done();

	/**
	 * Returns data inside the Context associated with the provided key.  See {@link #withValue}
	 * for more information on associating data with Contexts.
	 *
	 * @param  key a key value is associated with.
	 * @return     value associated with the key.
	 */
	public Object value(Object key);

	/**
	 * Returns a child of the current Context that can be canceled.  After <code>cancel()</code>
	 * is invoked, the counter returned by </code>done()</code> method of the new Context (and all
	 * Contexts further derived from it) will be set to zero.
	 *
	 * It is expected that the new Context will be canceled only by the caller that created it.
	 * This is in fact the reason the extended interface <code>CancelableContext</code> exists:
	 * to discourage <code>cancel()</code> from being invoked by anybody other than the creator.
	 *
	 * @return a child of the current Context that can be canceled.
	 */
	public CancelableContext withCancel();

	/**
	 * Returns a child of the current Context that is automatically canceled after the provided
	 * deadline is reached.  The returned Context can also be canceled manually; this is in fact
	 * encouraged when the Context is no longer needed in order to free up the timer resources.
	 *
	 * It is expected that the new Context will be (explicitly) canceled only by the caller that
	 * created it.  This is in fact the reason the extended interface <code>CancelableContext</code>
	 * exists: to discourage <code>cancel()</code> from being invoked by anybody other than the
	 * creator.
	 *
	 * @param  deadline an absolute time after which the Context will be canceled.
	 * @return          a child of the current Context that is automatically canceled after the
	 *                  provided deadline is reached.
	 */
	public CancelableContext withDeadline(DateTime deadline);

	/**
	 * Returns a child of the current Context that is automatically canceled after the provided
	 * duration of time.  The returned Context can also be canceled manually; this is in fact
	 * encouraged when the Context is no longer needed in order to free up the timer resources.
	 *
	 * It is expected that the new Context will be (explicitly) canceled only by the caller that
	 * created it.  This is in fact the reason the extended interface <code>CancelableContext</code>
	 * exists: to discourage <code>cancel()</code> from being invoked by anybody other than the
	 * creator.
	 *
	 * @param  timeout a duration of time after which the Context will be canceled.
	 * @return         a child of the current Context that is automatically canceled after the
	 *                 provided duration of time.
	 */
	public CancelableContext withTimeout(Duration timeout);

	/**
	 * Returns a child of the current Context that additionally contains the given key and its
	 * associated value.  A subsequent call to <code>value(key)</code> will return the provided
	 * <code>value</code>.  This method should be used only for data that is relevant across
	 * multiple API boundaries and not for passing extra parameters to functions and methods.
	 *
	 * Any type can be used as a key but the caller should preferrably use a private or protected
	 * type to prevent collisions (i.e., to prevent somebody else instantiating a key of the
	 * same type).  Keys are tested for equality by comparing their value pairs:
	 * <code>(getClass(), hashCode())</code>.  The caller shouldn't count on implementation
	 * maintaining a reference to the provided key.
	 *
	 * @param  key   a key value is associated with.
	 * @param  value a value associated with the key.
	 * @return       a child of the current Context that additionally contains the given key and its
	 *               associated value
	 */
	public Context withValue(Object key, Object value);
}
