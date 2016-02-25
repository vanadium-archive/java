// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.rx.syncbase;

import java.util.List;

import io.v.baku.toolkit.blessings.ClientUser;

public interface SyncHostLevel {
    String DEFAULT_CLOUD_SYNC_SUFFIX = "cloudsync",
            DEFAULT_SG_HOST_SUFFIX = "sghost",
            DEFAULT_RENDEZVOUS_SUFFIX = "sgmt";

    String getSyncgroupHostName(ClientUser clientUser);
    List<String> getRendezvousTableNames(ClientUser clientUser);
}
