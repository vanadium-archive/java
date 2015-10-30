// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.impl.google.lib.discovery;

import com.google.common.primitives.Bytes;

import java.nio.ByteBuffer;
import java.util.UUID;

import io.v.x.ref.lib.discovery.Uuid;

/**
 * A class for generating v5 UUIDs and converting from {@link UUID} and
 * {@link Uuid}.  {@link UUID} does not provide
 * any mechanism to compute v5 UUIDs which are used to convert InterfaceNames to service
 * UUIDs.  Conversion from {@link Uuid} and {@link UUID} is necessary because the VDL type
 * and the native type are annoying to convert to.
 */
public class UUIDUtil {
    public static native UUID UUIDForInterfaceName(String name);

    public static native UUID UUIDForAttributeKey(String key);

    /**
     * Converts from {@link UUID} to {@link Uuid}.
     */
    public static Uuid UUIDToUuid(UUID id) {
        ByteBuffer b = ByteBuffer.allocate(16);
        b.putLong(id.getMostSignificantBits());
        b.putLong(id.getLeastSignificantBits());
        return new Uuid(Bytes.asList(b.array()));
    }

    /**
     * Converts from {@link Uuid} to {@link UUID}
     */
    public static UUID UuidToUUID(Uuid id) {
        ByteBuffer b = ByteBuffer.wrap(Bytes.toArray(id));
        return new UUID(b.getLong(), b.getLong());
    }
}

