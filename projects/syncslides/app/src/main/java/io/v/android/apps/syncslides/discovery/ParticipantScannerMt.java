// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.syncslides.discovery;

import java.util.HashSet;
import java.util.Set;

import io.v.android.apps.syncslides.model.Participant;

/**
 * Scan a V23 MT for syncslides participants.
 */
public class ParticipantScannerMt implements ParticipantScanner {
    private static final String TAG = "ParticipantScannerMt";

    /**
     * Every v23 service will be mounted in the namespace with a name prefixed
     * by this.
     */
    public static String ROOT_NAME = "users/syncslides";

    /**
     * Used for V23 communication.
     */
    private final V23Manager mV23Manager;

    public ParticipantScannerMt(V23Manager v23Manager) {
        mV23Manager = v23Manager;
    }

    @Override
    public Set<Participant> scan() {
        Set<String> endPoints = mV23Manager.scan(ROOT_NAME + "/*");
        Set<Participant> result = new HashSet<>();
        for (String endPoint : endPoints) {
            result.add(new ParticipantPeer(endPoint));
        }
        return result;
    }
}
