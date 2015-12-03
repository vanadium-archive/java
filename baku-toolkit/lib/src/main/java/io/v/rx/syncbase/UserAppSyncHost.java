// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.rx.syncbase;

import android.content.Context;

import java.util.Arrays;
import java.util.List;

import io.v.baku.toolkit.blessings.BlessingsUtils;
import io.v.impl.google.naming.NamingUtil;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UserAppSyncHost implements SyncHostLevel {
    private final String mAppName, mSgHostSuffix, mRendezvousSuffix;

    public UserAppSyncHost(final Context androidContext) {
        this(androidContext.getPackageName(), DEFAULT_SG_HOST_SUFFIX, DEFAULT_RENDEZVOUS_SUFFIX);
    }

    @Override
    public String getSyncgroupHostName(final String username) {
        return NamingUtil.join(BlessingsUtils.userMount(username), mAppName, mSgHostSuffix);
    }

    @Override
    public List<String> getRendezvousTableNames(String username) {
        return Arrays.asList(NamingUtil.join(
                BlessingsUtils.userMount(username), mAppName, mRendezvousSuffix));
    }
}
