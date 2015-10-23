// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.syncslides.model;

import io.v.android.apps.syncslides.db.VPerson;

/**
 * Someone taking part in a presentation.
 *
 * An instance of this corresponds to one instance of a user running
 * syncslides.
 */
public interface Participant {

    // User presenting.
    VPerson getUser();

    // Deck the user may be presenting.
    Deck getDeck();

    // The presentation ID.
    String getPresentationId();

    // The syncgroup name.
    String getSyncgroupName();

    // Name of a service with participant information.
    String getServiceName();

    // Initially get or refresh data from the endPoint.
    // Return true if call succeeds, false otherwise.
    boolean refreshData();

    // For debugging.
    String toString();

    // For storage in Sets.
    boolean equals(Object obj);

    // For storage in Sets.
    int hashCode();

    /**
     * Keys for Bundle fields.
     */
    class B {
        public static final String SYNCGROUP_NAME = "participant_syncgroup_name";
        public static final String PRESENTATION_ID = "participant_pres_id";
        public static final String PARTICIPANT_ROLE = "participant_role";
        public static final String PARTICIPANT_SHOULD_ADV = "participant_is_advertising";
        public static final String PARTICIPANT_SYNCED = "participant_synced";
    }

    class Unknown {
        public static final String SYNCGROUP_NAME = "unknown_syncgroup";
        public static final String PRESENTATION_ID = "unknown_pres_id";
    }
}
