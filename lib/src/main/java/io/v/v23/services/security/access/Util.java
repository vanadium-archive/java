// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.services.security.access;

import com.google.common.reflect.TypeToken;

import io.v.v23.verror.VException;
import io.v.v23.vom.VomUtil;

/**
 * Util provides utilities for encoding/decoding various Veyron formats.  The encoding format
 * used here must match the encoding format used by the corresponding JNI Go library.
 */
class Util {
    /**
     * VOM-encodes the provided ACL.
     *
     * @param  acl  ACL to be encoded.
     * @return      VOM-encoded ACL.
     */
    static byte[] encodeACL(AccessList acl) throws VException {
        return VomUtil.encode(acl, new TypeToken<AccessList>(){}.getType());
    }

    /**
     * VOM-decodes the provided VOM-encoded ACL.
     *
     * @param  encoded         VOM-encoded ACL.
     * @return                 decoded ACL.
     * @throws VException      if the provided ACL couldn't be decoded.
     */
    static AccessList decodeACL(byte[] encoded) throws VException {
        if (encoded == null || encoded.length == 0) {
            return null;
        }
        return (AccessList) VomUtil.decode(encoded, new TypeToken<AccessList>(){}.getType());
    }
}