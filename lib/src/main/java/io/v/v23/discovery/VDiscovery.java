// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.discovery;

import java.util.List;

import io.v.v23.context.VContext;
import io.v.v23.security.BlessingPattern;

/**
 * An interface that exposes the Vanadium discovery interface.
 */
public interface VDiscovery {
    interface AdvertiseDoneCallback {
        /**
         * Called when the advertisement finishes.
         */
        void done();
    }

    interface ScanCallback {
        /**
         * Called when a new update for a Service that matches the scan query.
         * @param update
         */
        void handleUpdate(Update update);
    }

    /**
     * Advertises a service to a set of blessings.
     *
     * @param ctx a context that will be used to stop the advertisement.  The advertisement will end
     *            when the context is cancelled or timed out
     * @param service the service with the attributes to advertises
     * @param patterns a set of blessing patterns for whom this advertisement is meant.  Any entity
     *                 not matching a pattern here won't know what the advertisement is for
     * @param cb a callback that is notified when the advertisement is done (either because
     *           ctx.done returned or there was an error)
     */
    void advertise(VContext ctx, Service service, List<BlessingPattern> patterns, AdvertiseDoneCallback cb);

    /**
     * Scans for advertisements that match a query.
     *
     * @param ctx a context that will be used to stop the scan.  The scan will end when the context
     *            is cancelled or timed out.
     * @param query The query is a WHERE expression of syncQL queryagainst scanned services, where
     *              keys are InstanceUuids and values are Service. <br>
     *              Examples:
     *              <p><blockquote><pre>
     *                 v.InstanceName = "v.io/i"
     *                 v.InstanceName = "v.io/i" AND v.Attrs["a"] = "v"
     *                 v.Attrs["a"] = "v1" OR v.Attrs["a"] = "v2"</pre></blockquote></p>
     *              SyncQL tutorial at:
     *              https://github.com/vanadium/docs/blob/master/tutorials/syncql-tutorial.m
     * @param updateCb the callback that will be called when advertisements are found and lost.
     */
    void scan(VContext ctx, String query, ScanCallback updateCb);
}
