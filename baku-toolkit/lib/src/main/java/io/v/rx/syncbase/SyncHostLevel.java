// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.rx.syncbase;

import java.util.List;

public interface SyncHostLevel {
    String DEFAULT_SG_HOST_SUFFIX = "sghost", DEFAULT_RENDEZVOUS_SUFFIX = "sgmt";

    String getSyncgroupHostName(String username);
    List<String> getRendezvousTableNames(String username);
}
