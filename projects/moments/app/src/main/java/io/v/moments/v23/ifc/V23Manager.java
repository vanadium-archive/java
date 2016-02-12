// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.moments.v23.ifc;

import android.app.Activity;

import com.google.common.util.concurrent.FutureCallback;

import org.joda.time.Duration;

import java.util.List;

import io.v.v23.InputChannelCallback;
import io.v.v23.context.VContext;
import io.v.v23.discovery.Update;
import io.v.v23.security.BlessingPattern;
import io.v.v23.security.Blessings;
import io.v.v23.verror.VException;

/**
 * V23 functionality; service creation and discovery.
 */
public interface V23Manager {
    void init(
            Activity activity, FutureCallback<Blessings> blessingCallback);

    void shutdown();

    void scan(
            String query,
            Duration duration,
            FutureCallback<VContext> startupCallback,
            InputChannelCallback<Update> updateCallback,
            FutureCallback<Void> completionCallback);

    Advertiser makeAdvertiser(AdSupporter adSupporter,
                              Duration duration,
                              List<BlessingPattern> visibility);

    VContext makeServerContext(
            String mountName, Object server) throws VException;

    VContext contextWithTimeout(Duration timeout);

    List<String> makeServerAddressList(VContext serverCtx);
}
