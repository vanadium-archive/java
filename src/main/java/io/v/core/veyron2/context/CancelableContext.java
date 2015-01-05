package io.v.core.veyron2.context;

/**
 * CancelableContext is an extension of Context interface that allows the user to explicitly
 * cancel the context.
 */
public abstract class CancelableContext implements Context {
	/**
	 * Cancels the Context.  After this method is invoked, the counter returned by
	 * </code>done()</code> method of the new Context (and all Context further derived from it)
	 * will be set to zero.
	*/
	public abstract void cancel();

	/**
	 * Method that restricts all implementations of CancelableContext (and therefore Context)
	 * to the local package.
	 */
	abstract void implementationsOnlyInThisPackage();
}
