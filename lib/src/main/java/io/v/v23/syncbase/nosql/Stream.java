package io.v.v23.syncbase.nosql;

import io.v.v23.verror.VException;

/**
 * An interface for iterating through a collection of elements.
 *
 * The {@link java.util.Iterator} returned by the {@link java.lang.Iterable#iterator iterator}:
 * <p><ul>
 *     <li>can be created <strong>only</strong> once,
 *     <li>does not support {@link java.util.Iterator#remove remove}</li>,
 *     <li>stops the iteration early and gracefully if the {@link #cancel} method is invoked,</li>
 *     <li>may throw {@link RuntimeException} if the underlying iteration step throws
 *         a {@link VException}.  The {@link RuntimeException#getCause cause} of the
 *         {@link RuntimeException} will be the said {@link VException}.</li>
 * </ul>
 */
public interface Stream<T> extends Iterable<T> {
    /**
     * Notifies the stream provider that it can stop producing elements.  The client must call
     * {@link #cancel} if it does not iterate through all the elements.
     * <p>
     * This method is idempotent and can be called concurrently with a thread that is iterating.
     * <p>
     * This method causes the iterator to (gracefully) terminate early.
     */
    void cancel() throws VException;
}
