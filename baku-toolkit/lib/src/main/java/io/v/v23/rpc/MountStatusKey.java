// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.rpc;

import lombok.Value;
import lombok.experimental.Accessors;

@Accessors(prefix = "m")
@Value
public class MountStatusKey {
    String mName, mServer;

    public static MountStatusKey fromMountStatus(final MountStatus m) {
        return new MountStatusKey(m.getName(), m.getServer());
    }
}
