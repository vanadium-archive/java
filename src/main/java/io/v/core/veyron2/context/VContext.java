package io.v.core.veyron2.context;

import org.joda.time.DateTime;
import org.joda.time.Duration;

import java.util.concurrent.CountDownLatch;

/**
 * VContext is an interface for carrying deadlines, cancellation, and data across API
 * boundaries.
 *
 * Applications receive a context when they initialize the veyron environment and should generally
 * pass this context (or a derivitive) on to dependant operations.  Further contexts can be derived
 * from the original context to refine the environment.  For example if part of your operation
 * requires a finer deadline you can create a new context to handle that part:
 *
 * <code>
 *    final VContext ctx = V.init(null);
 *    // We'll use cacheCtx to lookup data in memcache; if it takes more than a
 *    // second to get data from memcache we should just skip the cache and
 *    // perform the slow operation.
 *    final VContext cacheCtx = ctx.withTimeout(Duration.standardSeconds(1));
 *    try {
 *    	fetchDataFromMemcache(cacheCtx, key);
 *    } catch (VException e) {
 *      if (e.equals(DEADLINE_EXCEEDED)) {
 *        recomputeData(ctx, key);
 *      }
 *    }
 * </code>
 *
 * VContexts form a tree where derived contexts are children of the contexts from which they were
 * derived.  Children inherit all the properties of their parent except for the property being
 * replaced (the deadline in the example above).
 *
 * VContexts are extensible.  The {@code value}/{@code withValue} methods allow you to
 * attach new information to the context and extend its capabilities. In the same way we derive
 * new contexts via the {@code with} family of functions you can create methods to attach new data:
 *
 * <code>
 *    public class Auth {
 *        private class AuthKey {}
 *
 *        private static final AuthKey KEY = new AuthKey();
 *
 *        public VContext withAuth(VContext parent, Auth data) {
 *          return parent.withValue(KEY, data);
 *        }
 *
 *        public Auth fromContext(VContext ctx) {
 *        	return (Auth)ctx.Value(KEY);
 *        }
 *    }
 * </code>
 *
 * Note that any type can be used as a key but the caller should preferrably use a private or
 * protected type to prevent collisions (i.e., to prevent somebody else instantiating a key of the
 * same type).  Keys are tested for equality by comparing their value pairs
 * {@code (getClass(), hashCode())}.
 */
public interface VContext {
	/**
	 * Returns the time at which this context will be automatically canceled, or {@code null}
	 * if this context doesn't have a deadline.
	 *
	 * @return the time at which this context will be automatically canceled or {@code null}
	 *         if there is no deadline.
	 */
	public DateTime deadline();

	/**
	 * Returns a counter that will reach the count of zero when this context is canceled (either
	 * explicitly or via an expired deadline).  Callers may block on this counter or periodically
	 * poll it.  When the counter reaches value zero, any blocked threads are released.
	 *
	 * Successive calls to this method must always return the same value.  Implementations may
	 * return {@code null} if they can never be canceled.
	 *
	 * @return a counter that reaches the count of zero when this context is canceled or exceeds
	 *         its deadline, or {@code null} if the counter can never be canceled.
	 */
	public CountDownLatch done();

	/**
	 * Returns data inside the context associated with the provided key.  See {@link #withValue}
	 * for more information on associating data with contexts.
	 *
	 * @param  key a key value is associated with.
	 * @return     value associated with the key.
	 */
	public Object value(Object key);

	/**
	 * Returns a child of the current context that can be canceled.  After {@code cancel()}
	 * is invoked on the new context, the counter returned by its {@code done()} method (and all
	 * contexts further derived from it) will be set to zero.
	 *
	 * It is expected that the new context will be canceled only by the caller that created it.
	 * This is the reason that the extended interface {@code CancelableVContext} exists:
	 * to discourage {@code cancel()} from being invoked by anybody other than the creator.
	 *
	 * @return a child of the current context that can be canceled.
	 */
	public CancelableVContext withCancel();

	/**
	 * Returns a child of the current context that is automatically canceled after the provided
	 * deadline is reached.  The returned context can also be canceled manually; this is in fact
	 * encouraged when the context is no longer needed in order to free up the timer resources.
	 *
	 * It is expected that the new context will be (explicitly) canceled only by the caller that
	 * created it.  This is the reason that the extended interface {@code CancelableVContext}
	 * exists: to discourage {@code cancel()} from being invoked by anybody other than the
	 * creator.
	 *
	 * @param  deadline an absolute time after which the context will be canceled.
	 * @return          a child of the current context that is automatically canceled after the
	 *                  provided deadline is reached.
	 */
	public CancelableVContext withDeadline(DateTime deadline);

	/**
	 * Returns a child of the current context that is automatically canceled after the provided
	 * duration of time.  The returned context can also be canceled manually; this is in fact
	 * encouraged when the context is no longer needed in order to free up the timer resources.
	 *
	 * It is expected that the new context will be (explicitly) canceled only by the caller that
	 * created it.  This is the reason that the extended interface {@code CancelableVContext}
	 * exists: to discourage {@code cancel()} from being invoked by anybody other than the
	 * creator.
	 *
	 * @param  timeout a duration of time after which the context will be canceled.
	 * @return         a child of the current context that is automatically canceled after the
	 *                 provided duration of time.
	 */
	public CancelableVContext withTimeout(Duration timeout);

	/**
	 * Returns a child of the current context that additionally contains the given key and its
	 * associated value.  A subsequent call to {@code value(key)} will return the provided
	 * {@code value}.  This method should be used only for data that is relevant across
	 * multiple API boundaries and not for passing extra parameters to functions and methods.
	 *
	 * Any type can be used as a key but the caller should preferrably use a private or protected
	 * type to prevent collisions (i.e., to prevent somebody else instantiating a key of the
	 * same type).  Keys are tested for equality by comparing their value pairs:
	 * {@code (getClass(), hashCode())}.  The caller shouldn't count on implementation
	 * maintaining a reference to the provided key.
	 *
	 * @param  key   a key value is associated with.
	 * @param  value a value associated with the key.
	 * @return       a child of the current context that additionally contains the given key and its
	 *               associated value
	 */
	public VContext withValue(Object key, Object value);
}
