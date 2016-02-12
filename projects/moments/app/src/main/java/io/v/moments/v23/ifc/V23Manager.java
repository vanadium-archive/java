// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.moments.v23.ifc;

import android.app.Activity;

import com.google.common.util.concurrent.FutureCallback;

import org.joda.time.Duration;

import java.util.List;

import io.v.v23.context.VContext;
import io.v.v23.security.BlessingPattern;
import io.v.v23.security.Blessings;

/**
 * V23 functionality; service creation and discovery.
 */
public interface V23Manager {
    /**
     * Start V23 runtime bound to the given activity, and give it a callback via
     * which it will get its blessings.
     */
    void init(Activity activity,
              FutureCallback<Blessings> blessingCallback);

    /**
     * Shutdown the v23 runtime.  This should be called in onDestroy to clean up
     * any lingering contexts associated with advertising or scanning.
     */
    void shutdown();

    /**
     * Used to construct RPCs.
     */
    VContext contextWithTimeout(Duration timeout);

    /**
     * Returns an advertiser that will start advertising using the adCampaign
     * for a fixed time duration.
     */
    Advertiser makeAdvertiser(AdCampaign adCampaign,
                              Duration duration,
                              List<BlessingPattern> visibility);

    /**
     * Returns a scanner that will look for advertisements matching the query,
     * for a fixed time duration.
     */
    Scanner makeScanner(String query, Duration duration);
}
