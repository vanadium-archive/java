// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.v23;

import android.security.KeyPairGeneratorSpec;

import io.v.v23.verror.VException;

import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.util.Calendar;

import javax.security.auth.x500.X500Principal;

/**
 * Convenience routines for working with keys stored in Android KeyStore.
 */
public class KeyStoreUtil {
    private static final String KEYSTORE = "AndroidKeyStore";
    private static final String PK_ALGORITHM = "EC";
    private static final int KEY_SIZE = 256;

    /**
     * Generates a new private key and stores it in the Android KeyStore under the provided alias.
     * If a key already exists under the provided alias, it will be overwritten.
     * Throws an exception if the key could not be generated.
     *
     * @param  ctx             android Context.
     * @param  alias           alias under which the private key will be stored inside the KeyStore.
     * @return                 an entry storing the private key and the certificate chain for the
     *                         corresponding public key.
     * @throws VException      if the key could not be generated.
     */
    public static KeyStore.PrivateKeyEntry genKeyStorePrivateKey(
        android.content.Context ctx, String alias) throws VException {
        try {
            // NOTE(spetrovic): KeyPairGenerator needs to be initialized with "RSA" algorithm and
            // not "EC" algorithm, even though we generate "EC" keys below.  Otherwise, Android
            // KeyStore claims that "EC" is an unrecognized algorithm!
            final KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA", KEYSTORE);
            final Calendar notBefore = Calendar.getInstance();
            final Calendar notAfter = Calendar.getInstance();
            notAfter.add(1, Calendar.YEAR);
            final KeyPairGeneratorSpec spec = new KeyPairGeneratorSpec.Builder(ctx)
                        .setAlias(alias)
                        .setKeyType(PK_ALGORITHM)
                        .setKeySize(KEY_SIZE)
                        .setSubject(new X500Principal(
                            String.format("CN=%s, OU=%s", alias, ctx.getPackageName())))
                        .setSerialNumber(BigInteger.ONE)
                        .setStartDate(notBefore.getTime())
                        .setEndDate(notAfter.getTime())
                        .build();
            keyGen.initialize(spec);
            keyGen.generateKeyPair();
            return getKeyStorePrivateKey(alias);
        } catch (NoSuchProviderException e) {
            throw new VException("Couldn't find Android KeyStore");
        } catch (NoSuchAlgorithmException e) {
            throw new VException(
                    "Keystore doesn't support " + PK_ALGORITHM + " algorithm: " + e.getMessage());
        } catch (InvalidAlgorithmParameterException e) {
            throw new VException("Invalid keystore algorithm parameters: " + e.getMessage());
        }
    }

    /**
     * Returns the private key if it exists in the Android KeyStore or {@code null} if it doesn't.
     * Throws an exception on an error.
     *
     * @param  alias           alias of the key in the KeyStore.
     * @return                 an entry storing the private key and the certificate chain for the
     *                         corresponding public key.
     * @throws VException      if the key could not be retrieved.
     */
    public static KeyStore.PrivateKeyEntry getKeyStorePrivateKey(String alias)
            throws VException {
        try {
            final KeyStore keyStore = KeyStore.getInstance(KEYSTORE);
            keyStore.load(null);
            final KeyStore.Entry entry = keyStore.getEntry(alias, null);
            if (entry == null) {
                return null;
            }
            if (!(entry instanceof KeyStore.PrivateKeyEntry)) {
                throw new VException(
                        "Entry " + alias + " exists but not a private key entry.");
            }
            return (KeyStore.PrivateKeyEntry)entry;
        } catch (KeyStoreException e) {
            throw new VException("KeyStore not initialized: " + e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            throw new VException("KeyStore doesn't support the algorithm: " + e.getMessage());
        } catch (IOException e) {
            throw new VException("Error loading keystore: " + e.getMessage());
        } catch (CertificateException e) {
            throw new VException("Error loading keystore certificates: " + e.getMessage());
        } catch (UnrecoverableEntryException e) {
            throw new VException("Couldn't get keystore entry: " + e.getMessage());
        }
    }

    private KeyStoreUtil() {}
}