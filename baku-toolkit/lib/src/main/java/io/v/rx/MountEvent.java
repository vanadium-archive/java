// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.rx;

import org.joda.time.DateTime;

import io.v.v23.verror.VException;
import java8.util.Optional;
import lombok.Value;
import lombok.experimental.Accessors;

@Accessors(prefix = "m")
@Value
public class MountEvent {
    public static MountEvent forAddNameSuccess(final String name) {
        return new MountEvent(true, name, Optional.empty(), DateTime.now(), Optional.empty());
    }

    public static MountEvent forAddNameFailure(final String name, final VException e) {
        return new MountEvent(true, name, Optional.empty(), DateTime.now(), Optional.of(e));
    }

    public static MountEvent forStatus(final boolean isMount, final String name,
                                       final String server, final DateTime timestamp,
                                       final VException error) {
        return new MountEvent(isMount, name, Optional.of(server), timestamp,
                Optional.ofNullable(error));
    }

    public static int compareByTimestamp(final MountEvent a, final MountEvent b) {
        return a.getTimestamp().compareTo(b.getTimestamp());
    }

    boolean mMount;
    String mName;
    Optional<String> mServer;
    DateTime mTimestamp;
    Optional<VException> mError;

    public boolean isSuccessfulMount() {
        return mMount && mServer.isPresent() && !mError.isPresent();
    }
}
