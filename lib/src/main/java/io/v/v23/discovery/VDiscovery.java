// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.discovery;

import java.util.List;

import io.v.v23.context.VContext;
import io.v.v23.security.BlessingPattern;

/**
 * An interface for discovery operations; it is the client-side library for the discovery service.
 */
public interface VDiscovery {
    /**
     * Callback invoked when the {@link #advertise advertisement} finishes.
     */
    interface AdvertiseDoneCallback {
        /**
         * Called when the {@link #advertise advertisement} finishes.
         */
        void done();
    }

    /**
     * Callback invoked with a {@link #scan scan} update.
     */
    interface ScanCallback {
        /**
         * Called with an update that matched the {@link #scan scan} query.
         */
        void handleUpdate(Update update);
    }

    /**
     * Advertises the service to be discovered by {@link #scan scan} implementations.
     * <p>
     * Visibility is used to limit the principals that can see the advertisement. An
     * empty list means that there are no restrictions on visibility (i.e, equivalent
     * to {@link io.v.v23.security.Constants#ALL_PRINCIPALS}).
     * <p>
     * Advertising will continue until the context is canceled or exceeds its deadline;  the
     * provided callback will be invoked when advertising stops.
     * <p>
     * It is an error to have simultaneously active advertisements for two identical
     * instances (i.e., {@link Service#instanceId}s).
     *
     * @param ctx a context that will be used to stop the advertisement; the advertisement will end
     *            when the context is cancelled or timed out
     * @param service the service with the attributes to advertises; this may be update with
     *            a random unique identifier if service.instanceId is not specified.
     * @param visibility a set of blessing patterns for whom this advertisement is meant; any entity
     *                   not matching a pattern here won't know what the advertisement is
     * @param cb a callback that is notified when the advertisement is done (either because
     *           context has expired or there was an error)
     */
    void advertise(VContext ctx, Service service, List<BlessingPattern> visibility,
                   AdvertiseDoneCallback cb);

    /**
     * Scans services that match the query and invokes the provided callback with updates.
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
     * @param ctx a context that will be used to stop the scan;  scan will end when the context
     *            is cancelled or timed out
     * @param query a WHERE expression of {@code syncQL query} against scanned services
     * @param updateCb the callback that will be called when a new advertisement is found or
     *                 when it is lost
     */
    void scan(VContext ctx, String query, ScanCallback updateCb);
}
