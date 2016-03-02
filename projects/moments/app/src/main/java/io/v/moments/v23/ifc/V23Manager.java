// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.moments.v23.ifc;

import android.app.Activity;

import com.google.common.util.concurrent.FutureCallback;

import org.joda.time.Duration;
import org.joda.time.ReadableDuration;

import io.v.v23.context.VContext;
import io.v.v23.security.Blessings;

/**
 * Secure distributed computing via underlying v23 APIs.
 *
 * This and other interfaces in the encompassing package comprise an API that
 * might feel more comfortable to java Android developers.  It wraps the
 * underlying static v23 methods in a framework of injectable, mockable
 * instances.
 */
public interface V23Manager {
    /**
     * Start V23 runtime bound to the given activity, and give it a callback via
     * which it will get its blessings.  This should be called on onCreate().
     *
     * When the blessings come in, the app can safely use v23 operations that
     * require a notion of identity, but until that time should make no attempt
     * to do so.
     */
    void init(Activity activity, FutureCallback<Blessings> blessingCallback);

    /**
     * Shutdown the v23 runtime.  This should be called in onDestroy() to cancel
     * any lingering contexts associated with v23 operations (advertising,
     * scanning, serving etc.), so that a subsequent call to init - say, during
     * destroy/create lifecycle event series - will start with clean state in
     * the v23 runtime.
     */
    void shutdown();

    /**
     * Used by v23 clients to make v23 RPCs.
     *
     * @param duration Amount of time until the operation self-cancels.
     */
    VContext contextWithTimeout(Duration duration);

    /**
     * Returns an advertiser bound to the given adCampaign.
     *
     * @param adCampaign Immutable description of the ad to run.
     * @return Advertiser that can start, stop and restart the advertisement.
     */
    Advertiser makeAdvertiser(AdCampaign adCampaign);

    /**
     * Returns a scanner that will look for advertisements matching the query.
     *
     * @param query Query limiting the ads that are processed.
     * @return Scanner that can start, stop and restart the scan.
     */
    Scanner makeScanner(String query);

    /**
     * Starts a server which invited remote users can connect to in order to inspect
     * application state (logs, statistics etc.).
     */
    void enableRemoteInspection();

    /**
     * Creates the invitation text to send to a remote user that authorizes them to
     * inspect the state of this application (logs, statistics etc.) using
     * <a href="https://godoc.org/v.io/x/ref/services/debug/debug">{@code debug browse}</a>.
     *
     * @param invitee identifier of the user to invite, typically their email address
     * @param duration duration for which this invitation is valid
     * @return A string containing the required credentials and instructions to access application
     * state
     */
    String inviteInspector(String invitee, ReadableDuration duration);
}
