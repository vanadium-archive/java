// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.security;

import io.v.v23.verror.VException;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.security.interfaces.ECPublicKey;
import java.util.Arrays;

class ECDSASigner implements Signer {
    private static final String HASH_ALGORITHM = "SHA-256";
    private static final String SIGN_ALGORITHM = "SHA256withECDSA";
    private static final String HASH_ALGORITHM_NAME = "SHA256";

    private final PrivateKey privKey;
    private final ECPublicKey pubKey;

    ECDSASigner(PrivateKey privKey, ECPublicKey pubKey) {
        this.privKey = privKey;
        this.pubKey = pubKey;
    }

    @Override
    public Signature sign(byte[] purpose, byte[] message) throws VException {
        message = CryptoUtil.messageDigest(HASH_ALGORITHM, message, purpose);
        // Sign.  Note that the signer will first apply another hash on the message, resulting in:
        // ECDSA.Sign(Hash(Hash(message) + Hash(purpose))).
        try {
            java.security.Signature sig = java.security.Signature.getInstance(SIGN_ALGORITHM);
            sig.initSign(this.privKey);
            sig.update(message);
            byte[] asn1Sig = sig.sign();
            return CryptoUtil.vanadiumSignature(HASH_ALGORITHM_NAME, purpose, asn1Sig);
        } catch (NoSuchAlgorithmException e) {
            throw new VException("Signing algorithm " + SIGN_ALGORITHM +
                " not supported by the runtime: " + e.getMessage());
        } catch (InvalidKeyException e) {
            throw new VException("Invalid private key: " + e.getMessage());
        } catch (SignatureException e) {
            throw new VException(
                "Invalid signing data [ " + Arrays.toString(message) + " ]: " + e.getMessage());
        }
    }

    @Override
    public ECPublicKey publicKey() {
        return this.pubKey;
    }
}
