// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.moments.lib;

import java.util.UUID;

/**
 * Consolidates different id styles under one type.
 *
 * Discovery demands strings, android RecyclerView demands longs.
 */
public class Id implements Comparable<Id> {
    private UUID mId;

    private Id(UUID id) {
        mId = id;
    }

    public static Id makeRandom() {
        return new Id(UUID.randomUUID());
    }

    public static Id fromString(String id) {
        return new Id(UUID.fromString(id));
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof Id)) {
            return false;
        }
        return mId.equals(((Id) obj).mId);
    }

    public int hashCode() {
        return mId.hashCode();
    }

    public String toString() {
        return mId.toString();
    }

    public Long toLong() {
        return mId.getLeastSignificantBits();
    }

    @Override
    public int compareTo(Id another) {
        return mId.compareTo(another.mId);
    }
}
