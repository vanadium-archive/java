// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.moments.v23.ifc;

import java.util.List;

import io.v.v23.discovery.AdId;
import io.v.v23.discovery.Attachments;
import io.v.v23.discovery.Attributes;
import io.v.v23.security.BlessingPattern;

/**
 * Provides the data needed to run an advertisement.
 */
public interface AdCampaign {
    /**
     * Unique Id associated with the advertisement, used to discriminate when an
     * ad is found or lost.
     */
    AdId getId();

    /**
     * Optional service interface name, can be used in query discrimination. The
     * name, if provided, should be the name of the service interface associated
     * with the result of #makeServer().
     */
    String getInterfaceName();

    /**
     * Map of 'smallish' name/value pairs to send with the advertisement.
     */
    Attributes getAttributes();

    /**
     * Larger blobs of data to made available asynchronously to scanners.
     */
    Attachments getAttachments();

    /**
     * Makes an instance of a service (a set of handlers) that will be run
     * during the life of the advertisement.
     *
     * I.e., every time an advertisement is started using this campaign, this
     * factory method will be called to create a new service object with a clean
     * state.  The service object will be used to start an actual server that
     * will serve requests only as long as the advertisement.
     *
     * If null returned, no server is launched.
     */
    Object makeService();

    /**
     * Name at which the service associated with #makeService() should be
     * mounted. Can be empty.
     */
    String getMountName();

    /**
     * A set of blessing patterns for whom this advertisement is meant; any
     * entity not matching a pattern here won't see the advertisement.
     */
    List<BlessingPattern> getVisibility();
}
