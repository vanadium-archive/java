package io.v.core.veyron2.services.security.access;

import com.google.common.reflect.TypeToken;

import io.v.core.veyron2.util.VomUtil;
import io.v.core.veyron2.verror.VException;

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
    static byte[] encodeACL(ACL acl) throws VException {
        return VomUtil.encode(acl, new TypeToken<ACL>(){}.getType());
    }

    /**
     * VOM-decodes the provided VOM-encoded ACL.
     *
     * @param  encoded         VOM-encoded ACL.
     * @return                 decoded ACL.
     * @throws VException      if the provided ACL couldn't be decoded.
     */
    static ACL decodeACL(byte[] encoded) throws VException {
        if (encoded == null || encoded.length == 0) {
            return null;
        }
        return (ACL) VomUtil.decode(encoded, new TypeToken<ACL>(){}.getType());
    }
}