// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.syncslides;

/**
 * Represents the user's type of participation in the presentation.
 */
public enum Role {
    /**
     * The user is actively presenting the deck to the audience.
     */
    PRESENTER,
    /**
     * The user is viewing a live presentation.
     */
    AUDIENCE,
    /**
     * The user is browsing through a presentation that they have seen before.  It is possible to
     * transition to PRESENTER.
     */
    BROWSER
}
