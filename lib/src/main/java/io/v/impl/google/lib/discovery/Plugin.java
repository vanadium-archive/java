// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.impl.google.lib.discovery;

import io.v.x.ref.lib.discovery.AdInfo;

/**
 * An interface for discovery plugins in Java.
 */
public interface Plugin {
    /**
     * Starts the advertisement of {@link adInfo}.
     * <p>
     * The advertisement will not be changed while it is being advertised.
     * <p>
     * If the advertisement is too large, the plugin may drop any information except
     * {@code id}, {@code interfaceName}, {@code hash}, and {@code dirAddrs}.
     * <p>
     *
     * @param adInfo      an advertisement to advertises
     * @throws Exception  if advertising couldn't be started
     */
    void startAdvertising(AdInfo adInfo) throws Exception;

    /**
     * Stops the advertisement of {@link adInfo}.
     *
     * @param adInfo      the advertisement to stop advertising
     * @throws Exception  if advertising couldn't be stopped
     */
    void stopAdvertising(AdInfo adInfo) throws Exception;

    /**
     * An interface for passing scanned advertisements.
     */
    public interface ScanHandler {
        /**
         * Called with each discovery update.
         */
        void handleUpdate(AdInfo adinfo);
    }

    /**
     * Starts a scan looking for advertisements that match the interface name.
     * <p>
     * An empty interface name means any advertisements.
     * <p>
     * Advertisements that are returned through {@link handler} can be changed.
     * The plugin should not reuse the returned advertisement.
     * <p>
     *
     * @param interfaceName an interface name to scan
     * @param handler       a handler to return updates of matched advertisements.
     * @throws Exception    if scanning couldn't be started
     */
    void startScan(String interfaceName, ScanHandler handler) throws Exception;

    /**
     * Stops the scanning associated with the given handler.
     *
     * @param handler       the handler to stop scanning for.
     * @throws Exception    if scanning couldn't be started
     */
    void stopScan(ScanHandler handler) throws Exception;

    /**
     * Closes the plugin.
     * <p>
     * This will be called after all active tasks have been cancelled.
     */
    void close();
}
