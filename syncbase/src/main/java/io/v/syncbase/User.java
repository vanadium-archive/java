// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase;

/**
 * Represents a user.
 */
public class User {
    public static final String PROVIDER_GOOGLE = "google";
    public static final String PROVIDER_NONE = "";

    private String mAlias;
    private String mProvider;

    public User(String alias) {
        mAlias = alias;
        mProvider= PROVIDER_GOOGLE; // TODO(alexfandrianto): Compute/validate the provider.
    }

    public User(String alias, String provider) {
        mAlias = alias;
        mProvider = provider;
    }

    /**
     * Returns a human-readable string for the user, such as a nickname or email address.
     * The value format depends on the OAuth provider.
     * If "google" is the provider, then the alias is an email address.
     */
    public String getAlias() {
        return mAlias;
    }

    /**
     * Returns the user's OAuth provider.
     */
    public String getOAuthProvider() {
        return mProvider;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof User) {
            User otherUser = (User) other;
            return mAlias.equals(otherUser.mAlias) && mProvider.equals(otherUser.mProvider);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int result = 1;
        int prime = 31;
        result = prime * result + (mAlias.hashCode());
        result = prime * result + (mProvider.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "User(alias: " + mAlias + ", provider: " + mProvider + ")";
    }
}
