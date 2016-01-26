// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.impl.google.lib.discovery;


import com.google.common.primitives.Bytes;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import io.v.x.ref.lib.discovery.EncryptionKey;

/**
 * A utility to encode and decode fields in io.v.v23.Service fields for use in discovery.
 *
 * TODO(bjornick,jhahn): Consider to share v.io/x/ref/lib/discovery/encoding.go through jni.
 */
public class EncodingUtil {
    static final Charset UTF8_CHARSET = Charset.forName("UTF-8");

    private static void writeUint(OutputStream out, int x) throws IOException {
        while ((x & 0xffffff80) != 0) {
            out.write((x & 0x7f) | 0x80);
            x >>>= 7;
        }
        out.write(x);
    }

    private static int readUint(InputStream in) throws IOException {
        for (int x = 0, s = 0; ;) {
            int b = in.read();
            if (b == -1) {
                throw new EOFException();
            }
            if ((b & 0x80) == 0) {
                return x | (b << s);
            }
            x |= (b & 0x7f) << s;
            s += 7;
            if (s > 35) {
                throw new IOException("overflow");
            }
        }
    }

    /**
     * Encodes the addresses passed in.
     *
     * @param addrs the list of addresses to encode.
     * @return the byte representation of the encoded addresses.
     * @throws IOException if the address can't be encoded.
     */
    public static byte[] packAddresses(List<String> addrs) throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        for (String addr : addrs) {
            writeUint(stream, addr.length());
            stream.write(addr.getBytes(UTF8_CHARSET));
        }
        return stream.toByteArray();
    }

    /**
     * Decodes addresses from a byte array that was encoded by packAddresses
     *
     * @param input the byte array toe decode
     * @return the list of addresses.
     * @throws IOException if the addresses can't be decoded.
     */
    public static List<String> unpackAddresses(byte[] input) throws IOException {
        ByteArrayInputStream stream = new ByteArrayInputStream(input);
        List<String> output = new ArrayList<>();
        while (stream.available() > 0) {
            int size = readUint(stream);
            byte[] data = new byte[size];
            int read = stream.read(data);
            if (read != size) {
                throw new EOFException();
            }
            output.add(new String(data, UTF8_CHARSET));
        }
        return output;
    }

    /**
     * Encode the encryption keys and algorithm passed in.
     *
     * @param encryptionAlgorithm the encryption algorithm to use.
     *                            See io.v.x.ref.lib.discovery.Constants for valid values.
     * @param keys the keys to encode
     * @return the byte array that is the encoded form.
     * @throws IOException if the keys can't be encoded.
     */
    public static byte[] packEncryptionKeys(int encryptionAlgorithm, List<EncryptionKey> keys)
            throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        writeUint(stream, encryptionAlgorithm);
        for (EncryptionKey key : keys) {
            byte[] byteKey = Bytes.toArray(key);
            writeUint(stream, byteKey.length);
            stream.write(byteKey);
        }
        return stream.toByteArray();
    }

    /**
     * Decodes the encryption algorithm and keys that was encoded by packEncryptionKeys.
     *
     * @param input the byte array containg the keys.
     * @return the keys and the encryption algorithm in input.
     * @throws IOException if the keys can't be decoded.
     */
    public static KeysAndAlgorithm unpackEncryptionKeys(byte[] input) throws IOException {
        ByteArrayInputStream stream = new ByteArrayInputStream(input);
        int algo = readUint(stream);
        List<EncryptionKey> keys = new ArrayList<>();
        while (stream.available() > 0) {
            int size = readUint(stream);
            byte[] key = new byte[size];
            int read = stream.read(key);
            if (read != size) {
                throw new EOFException();
            }
            keys.add(new EncryptionKey(Bytes.asList(key)));
        }
        return new KeysAndAlgorithm(algo, keys);
    }

    /**
     * Stores {@link EncryptionKey}s and the encryption algorithm.
     */
    public static class KeysAndAlgorithm {
        int encryptionAlgorithm;
        List<EncryptionKey> keys;

        /**
         * Returns the stored encryption algorithm.
         */
        public int getEncryptionAlgorithm() {
            return encryptionAlgorithm;
        }

        /**
         * Returns the stored keys.
         */
        public List<EncryptionKey> getKeys() {
            return keys;
        }

        KeysAndAlgorithm(int encryptionAlgo, List<EncryptionKey> keys) {
            encryptionAlgorithm = encryptionAlgo;
            this.keys = keys;
        }
    }
}
