// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase;

/**
 * Represents a user.
 */
public class User {
    /**
     * Returns a human-readable string for the user, such as a nickname or email address.
     * The value format depends on the OAuth provider.
     * If "google" is the provider, then the alias is an email address.
     */
    public String getAlias() {
        throw new RuntimeException("Not implemented");
    }

    /**
     * Returns the user's OAuth provider.
     */
    public String getOAuthProvider() {
        // TODO(alexfandrianto): Define constants for providers (e.g., "google", "facebook", etc.).
        throw new RuntimeException("Not implemented");
    }
}
