// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.security;

import io.v.v23.verror.VException;
import io.v.v23.vom.VomUtil;

import java.security.interfaces.ECPublicKey;

class Util {
    /**
     * VOM-encodes the provided Signature.
     *
     * @param  signature Signature to be encoded.
     * @return           the encoded Signature.
     */
    static byte[] encodeSignature(Signature signature) throws VException {
        return VomUtil.encode(signature, Signature.class);
    }

    /**
     * VOM-decodes the VOM-encoded Signature.
     *
     * @param  encoded         VOM-encoded Signature.
     * @return                 decoded Signature.
     * @throws VException      if the provided Signature couldn't be decoded.
     */
    static Signature decodeSignature(byte[] encoded) throws VException {
        if (encoded == null || encoded.length == 0) {
            return null;
        }
        return (Signature) VomUtil.decode(encoded, Signature.class);
    }

    /**
     * DER-encodes the provided ECPublicKey.
     *
     * @param  key ECPublicKey to be encoded.
     * @return     the encoded ECPublicKey.
     */
    static byte[] encodePublicKey(ECPublicKey key) {
        if (key == null) {
            return new byte[0];
        }
        return key.getEncoded();
    }

    /**
     * Decodes the DER-encoded ECPublicKey.
     *
     * @param  encoded         DER-encoded ECPublicKey.
     * @return                 decoded ECPublicKey.
     * @throws VException      if the provided ECPublicKey couldn't be decoded.
     */
    static ECPublicKey decodePublicKey(byte[] encoded) throws VException {
        if (encoded == null || encoded.length == 0) {
            return null;
        }
        return CryptoUtil.decodeECPublicKey(encoded);
    }
}