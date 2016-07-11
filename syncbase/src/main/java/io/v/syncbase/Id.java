// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase;

/**
 * Uniquely identifies a database, collection, or syncgroup.
 */
public class Id {
    private final io.v.syncbase.core.Id mId;

    Id(io.v.syncbase.core.Id id) {
        mId = id;
    }

    Id(String blessing, String name) {
        mId = new io.v.syncbase.core.Id(blessing, name);
    }

    // TODO(sadovsky): Replace encode and decode method implementations with calls to Cgo.
    private static final String SEPARATOR = ",";

    /**
     * @throws IllegalArgumentException if invalid encodedId
     */
    public static Id decode(String encodedId) {
        String[] parts = encodedId.split(SEPARATOR);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid encoded ID: \"" + encodedId + "\"");
        }
        return new Id(parts[0], parts[1]);
    }

    public String encode() {
        return mId.encode();
    }

    io.v.syncbase.core.Id toCoreId() {
        return mId;
    }

    String getBlessing() {
        return mId.blessing;
    }

    public String getName() {
        return mId.name;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Id) {
            Id otherId = (Id) other;
            return mId.blessing.equals(otherId.getBlessing()) && mId.name.equals(otherId.getName());
        }
        return false;
    }

    @Override
    public int hashCode() {
        // Note: Copied from VDL.
        int result = 1;
        int prime = 31;

        result = prime * result + (mId.blessing == null ? 0 : mId.blessing.hashCode());

        result = prime * result + (mId.name == null ? 0 : mId.name.hashCode());

        return result;
    }

    @Override
    public String toString() {
        return "Id(" + encode() + ")";
    }
}
