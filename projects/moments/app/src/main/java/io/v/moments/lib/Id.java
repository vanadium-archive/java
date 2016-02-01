// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.moments.lib;

import java.util.Random;

/**
 * Consolidates different id styles under one type.
 *
 * Discovery demands strings, android RecyclerView demands longs.
 */
public class Id implements Comparable<Id> {
    private Long mId;
    private static final Random RANDOM = new Random();

    private Id(Long id) {
        mId = id;
    }

    public static Id makeRandom() {
        long lng = RANDOM.nextLong();
        // Keep it positive to assure roundtrip to string works simply.
        return new Id(lng < 0 ? -lng : lng);
    }

    public static Id fromString(String id) {
        return new Id(Long.parseLong(id, 16));
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
        return Long.toHexString(mId);
    }

    public Long toLong() {
        return mId;
    }

    @Override
    public int compareTo(Id another) {
        return mId.compareTo(another.mId);
    }
}
