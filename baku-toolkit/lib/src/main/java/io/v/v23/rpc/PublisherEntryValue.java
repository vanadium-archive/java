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
public class PublisherEntryValue {
    DateTime mLastMount;
    VException mLastMountError;
    Duration mTtl;
    DateTime mLastUnmount;
    VException mLastUnmountError;

    public static PublisherEntryValue fromPublisherEntry(final PublisherEntry m) {
        return new PublisherEntryValue(m.getLastMount(), m.getLastMountError(), m.getTTL(),
                m.getLastUnmount(), m.getLastUnmountError());
    }
}
