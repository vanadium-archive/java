// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.rpc;

import org.joda.time.DateTime;
import org.joda.time.Duration;

import io.v.v23.verror.VException;
import lombok.Value;
import lombok.experimental.Accessors;

@Accessors(prefix = "m")
@Value
public class MountStatusValue {
    DateTime mLastMount;
    VException mLastMountError;
    Duration mTtl;
    DateTime mLastUnmount;
    VException mLastUnmountError;

    public static MountStatusValue fromMountStatus(final MountStatus m) {
        return new MountStatusValue(m.getLastMount(), m.getLastMountError(), m.getTTL(),
                m.getLastUnmount(), m.getLastUnmountError());
    }
}
