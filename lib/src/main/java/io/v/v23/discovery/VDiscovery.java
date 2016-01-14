// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.discovery;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;

import javax.annotation.CheckReturnValue;

import io.v.v23.InputChannel;
import io.v.v23.context.VContext;
import io.v.v23.security.BlessingPattern;

/**
 * An interface for discovery operations; it is the client-side library for the discovery service.
 */
public interface VDiscovery {
    /**
     * Advertises the service to be discovered by {@link #scan scan} implementations.
     * <p>
     * Returns a new {@link ListenableFuture} that completes once advertising starts.  The result
     * of this future is a new {@link ListenableFuture} that completes once advertising stops.
     * Once successfully started, advertising will continue until the context is canceled or
     * exceeds its deadline.  Note that the future signaling a completion of advertising can
     * never fail.
     * <p>
     * Visibility is used to limit the principals that can see the advertisement. An
     * empty list means that there are no restrictions on visibility (i.e, equivalent
     * to {@link io.v.v23.security.Constants#ALL_PRINCIPALS}).
     * <p>
     * If {@link Service#instanceId} is not specified, a random 128 bit (16 byte) {@code UUID} will
     * be assigned to it once advertising starts.  Any change to service will not be applied after
     * advertising starts.
     * <p>
     * It is an error to have simultaneously active advertisements for two identical
     * instances (i.e., {@link Service#instanceId}s).
     * <p>
     * The returned future is guaranteed to be executed on an {@link java.util.concurrent.Executor}
     * specified in {@code context} (see {@link io.v.v23.V#withExecutor}).
     * <p>
     * The returned future will fail with {@link java.util.concurrent.CancellationException} if
     * {@code context} gets canceled.
     *
     * @param context    a context that will be used to stop the advertisement; the advertisement
     *                   will end when the context is cancelled or timed out
     * @param service    the service with the attributes to advertises; this may be update with
     *                   a random unique identifier if service.instanceId is not specified.
     * @param visibility a set of blessing patterns for whom this advertisement is meant; any entity
     *                   not matching a pattern here won't know what the advertisement is
     * @return           a new {@link ListenableFuture} that completes once advertising starts;
     *                   the result of this future is a second {@link ListenableFuture} that
     *                   completes once advertising stops
     */
    @CheckReturnValue
    ListenableFuture<ListenableFuture<Void>> advertise(
            VContext context, Service service, List<BlessingPattern> visibility);

    /**
     * Scans services that match the query and returns an {@link InputChannel} of updates.
     * <p>
     * Scanning will continue until the context is canceled or exceeds its deadline.
     * <p>
     * The query is a {@code WHERE} expression of {@code syncQL} query against scanned services,
     * where keys are {@link Service#instanceId}s and values are {@link Service}s.
     * <p>
     * Examples:
     * <p><blockquote><pre>
     *     v.InstanceName = "v.io/i"
     *     v.InstanceName = "v.io/i" AND v.Attrs["a"] = "v"
     *     v.Attrs["a"] = "v1" OR v.Attrs["a"] = "v2"
     * </pre></blockquote><p>
     * You can find the {@code SyncQL} tutorial at:
     *     https://github.com/vanadium/docs/blob/master/tutorials/syncql-tutorial.md
     *
     * @param context  a context that will be used to stop the scan;  scan will end when the context
     *                 is cancelled or timed out
     * @param query    a WHERE expression of {@code syncQL query} against scanned services
     * @return         a (potentially-infite) {@link InputChannel} of updates
     */
    @CheckReturnValue
    InputChannel<Update> scan(VContext context, String query);
}
