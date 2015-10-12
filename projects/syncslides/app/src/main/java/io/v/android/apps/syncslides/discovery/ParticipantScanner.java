// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.syncslides.discovery;

import java.util.Set;

import io.v.android.apps.syncslides.model.Participant;

/**
 * Scans the network for other Participants.
 */
public interface ParticipantScanner {
    /**
     * @return all participants found.
     */
    Set<Participant> scan();
}
