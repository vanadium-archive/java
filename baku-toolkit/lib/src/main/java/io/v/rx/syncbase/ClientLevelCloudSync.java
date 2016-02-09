// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.rx.syncbase;

import java.util.Arrays;
import java.util.List;

import io.v.baku.toolkit.blessings.BlessingsUtils;
import io.v.baku.toolkit.blessings.ClientUser;
import io.v.impl.google.naming.NamingUtil;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ClientLevelCloudSync implements SyncHostLevel {
    public static final ClientLevelCloudSync DEFAULT =
            new ClientLevelCloudSync(DEFAULT_CLOUD_SYNC_SUFFIX, DEFAULT_RENDEZVOUS_SUFFIX);

    private final String mSgHostSuffix, mRendezvousSuffix;

    @Override
    public String getSyncgroupHostName(final ClientUser clientUser) {
        return NamingUtil.join(BlessingsUtils.clientMount(clientUser.getClientId()), mSgHostSuffix);
    }

    @Override
    public List<String> getRendezvousTableNames(final ClientUser clientUser) {
        return Arrays.asList(NamingUtil.join(
                BlessingsUtils.clientMount(clientUser.getClientId()), mRendezvousSuffix));
    }
}
