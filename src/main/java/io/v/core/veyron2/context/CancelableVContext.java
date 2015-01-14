package io.v.core.veyron2.context;

/**
 * CancelableVContext is an extension of {@code VContext} interface that allows the user to
 * explicitly cancel the context.
 */
public abstract class CancelableVContext implements VContext {
	/**
	 * Cancels the contex.  After this method is invoked, the counter returned by {@code done()}
	 * method of the new context (and all contexts further derived from it) will be set to zero.
	*/
	public abstract void cancel();

	/**
	 * Method that restricts all implementations of {@code CancelableVContext}
	 * (and therefore {@code VContext}) to the local package.
	 */
	abstract void implementationsOnlyInThisPackage();
}
