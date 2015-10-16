// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.syncslides.model;

import android.os.Bundle;

/**
 * Someone taking part in a presentation.
 *
 * An instance of this corresponds to one instance of a user running
 * syncslides.
 */
public interface Participant {
    /**
     * If true, enable MT-based (mounttable) discovery. Deck view will scan a MT
     * to find live presentations. Clicking play on a presentation will start a
     * service and try to mount it in a MT so other deck views can list it in
     * the UX. MT location determined in
     * {@link io.v.android.apps.syncslides.discovery.V23Manager}.
     */
    boolean ENABLE_MT_DISCOVERY = false;

    // Name of the user participating, intended to be visible to others. This
    // can be a colloquial name as opposed to a 'real' name or email address
    // extracted from a device or blessing.
    String getUserName();

    // Deck the user may be presenting.
    Deck getDeck();

    // Name of a service with participant information.
    String getEndPoint();

    // Initially get or refresh data from the endPoint.
    void refreshData();

    // Serializable form of this for storing, passing via Message, etc.
    Bundle toBundle();

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
        public static final String PARTICIPANT_ROLE = "participant_role";
        public static final String PARTICIPANT_END_POINT = "participant_endPoint";
        public static final String PARTICIPANT_NAME = "participant_name";
    }
}
