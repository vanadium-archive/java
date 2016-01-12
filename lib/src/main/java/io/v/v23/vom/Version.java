// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.vom;

public enum Version {
    Version80((byte)0x80),
    Version81((byte)0x81);

    Version(byte versionByte) {
        this.versionByte = versionByte;
    }

    private byte versionByte;

    public byte AsByte() {
        return versionByte;
    }

    public static Version FromByte(byte b) {
        for (Version v : Version.values()) {
            if (v.AsByte() == b) {
                return v;
            }
        }
        throw new RuntimeException("invalid version byte " + b);
    }

    public static Version DefaultVersion = Version80;

}
