// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.context;

import org.joda.time.DateTime;
import org.joda.time.Duration;

import java.util.concurrent.CountDownLatch;

/**
 * The interface for carrying deadlines, cancellation, as well as other arbitrary values.
 * <p>
 * Application code receives contexts in two main ways:
 * <p><ol>
 * <li> A {@link VContext} is returned by {@link io.v.v23.V#init V.init}.  This will generally be
 * used to set up servers in {@code main}, or for stand-alone client programs:
 * <p><blockquote><pre>
 *     public static void main(String[] args) {
 *         VContext ctx = V.init();
 *         doSomething(ctx);
 *     }
 * </pre></blockquote><p></li>
 * <li> A {@link VContext} is passed to every server method implementation as the first parameter:
 * <p><blockquote><pre>
 *     public class MyServerImpl implements MyServer {
 *         {@literal @}Override
 *         public void get(VContext ctx, ServerCall call) throws VException;
 *     }
 * </pre></blockquote><p></li></ol>
 * Further contexts can be derived from the original context to refine the environment.
 * For example if part of your operation requires a finer deadline you can create a new context to
 * handle that part:
 * <p><blockquote><pre>
 *    VContext ctx = V.init();
 *    // We'll use cacheCtx to lookup data in memcache; if it takes more than a
 *    // second to get data from memcache we should just skip the cache and
 *    // perform the slow operation.
 *    VContext cacheCtx = ctx.withTimeout(Duration.standardSeconds(1));
 *    try {
 *        fetchDataFromMemcache(cacheCtx, key);
 *    } catch (VException e) {
 *      if (e.is(DEADLINE_EXCEEDED)) {
 *        recomputeData(ctx, key);
 *      }
 *    }
 * </pre></blockquote><p>
 * {@link VContext}s form a tree where derived contexts are children of the contexts from which
 * they were derived.  Children inherit all the properties of their parent except for the property 
 * being replaced (the deadline in the example above).
 * <p>
 * {@link VContext}s are extensible.  The {@link #value value}/{@link #withValue withValue} methods
 * allow you to attach new information to the context and extend its capabilities. In the same way
 * we derive new contexts via the {@code with} family of functions you can create methods to attach
 * new data:
 * <p><blockquote><pre>
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
 *            return (Auth)ctx.Value(KEY);
 *        }
 *    }
 * </pre></blockquote><p>
 * Note that any type can be used as a key but the caller should preferrably use a private or
 * protected type to prevent collisions (i.e., to prevent somebody else instantiating a key of the
 * same type).  Keys are tested for equality by comparing their value pairs
 * {@code (getClass(), hashCode())}.
 */
public interface VContext {
    /**
     * Returns the time at which this context will be automatically canceled, or {@code null}
     * if this context doesn't have a deadline.
     */
    DateTime deadline();

    /**
     * Returns a counter that will reach the count of zero when this context is canceled (either
     * explicitly or via an expired deadline).  Callers may block on this counter or periodically
     * poll it.  When the counter reaches value zero, any blocked threads are released.
     * <p>
     * Successive calls to this method must always return the same value.  Implementations may
     * return {@code null} if they can never be canceled.
     *
     * @return a counter that reaches the count of zero when this context is canceled or exceeds
     *         its deadline, or {@code null} if the counter can never be canceled.
     */
    CountDownLatch done();

    /**
     * Returns data inside the context associated with the provided key.  See {@link #withValue}
     * for more information on associating data with contexts.
     *
     * @param  key a key value is associated with.
     * @return     value associated with the key.
     */
    Object value(Object key);

    /**
     * Returns a child of the current context that can be canceled.  After {@link #cancel cancel}
     * is invoked on the new context, the counter returned by its {@link #done done} method (and all
     * contexts further derived from it) will be set to zero.
     * <p>
     * It is expected that the new context will be canceled only by the caller that created it.
     * This is the reason that the extended interface {@link CancelableVContext} exists:
     * to discourage {@link #cancel cancel} from being invoked by anybody other than the creator.
     *
     * @return a child of the current context that can be canceled.
     */
    CancelableVContext withCancel();

    /**
     * Returns a child of the current context that is automatically canceled after the provided
     * deadline is reached.  The returned context can also be canceled manually; this is in fact
     * encouraged when the context is no longer needed in order to free up the timer resources.
     * <p>
     * It is expected that the new context will be (explicitly) canceled only by the caller that
     * created it.  This is the reason that the extended interface {@link CancelableVContext}
     * exists: to discourage {@link #cancel cancel} from being invoked by anybody other than the
     * creator.
     *
     * @param  deadline an absolute time after which the context will be canceled.
     * @return          a child of the current context that is automatically canceled after the
     *                  provided deadline is reached.
     */
    CancelableVContext withDeadline(DateTime deadline);

    /**
     * Returns a child of the current context that is automatically canceled after the provided
     * duration of time.  The returned context can also be canceled manually; this is in fact
     * encouraged when the context is no longer needed in order to free up the timer resources.
     * <p>
     * It is expected that the new context will be (explicitly) canceled only by the caller that
     * created it.  This is the reason that the extended interface {@link CancelableVContext}
     * exists: to discourage {@link #cancel cancel} from being invoked by anybody other than the
     * creator.
     *
     * @param  timeout a duration of time after which the context will be canceled.
     * @return         a child of the current context that is automatically canceled after the
     *                 provided duration of time.
     */
    CancelableVContext withTimeout(Duration timeout);

    /**
     * Returns a child of the current context that additionally contains the given key and its
     * associated value.  A subsequent call to {@link #value value} will return the provided
     * value.  This method should be used only for data that is relevant across multiple API
     * boundaries and not for passing extra parameters to functions and methods.
     * <p>
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
    VContext withValue(Object key, Object value);
}
