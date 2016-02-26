// Copyright 2016 The Vanadium Authors. All rights reserved.
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

import io.v.x.ref.lib.discovery.EncryptionAlgorithm;
import io.v.x.ref.lib.discovery.EncryptionKey;

/**
 * A utility to encode and decode fields in io.v.v23.Service fields for use in discovery.
 */
public class EncodingUtil {
    // We use "ISO8859-1" to preserve data in a string without interpretation.
    private static final Charset ENC = Charset.forName("ISO8859-1");

    private static void writeUint(OutputStream out, int x) throws IOException {
        while ((x & 0xffffff80) != 0) {
            out.write((x & 0x7f) | 0x80);
            x >>>= 7;
        }
        out.write(x);
    }

    private static int readUint(InputStream in) throws IOException {
        for (int x = 0, s = 0; ; ) {
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
     * @param addrs         the list of addresses to encode
     * @return              the byte representation of the encoded addresses
     * @throws IOException  if the address can't be encoded
     */
    public static byte[] packAddresses(List<String> addrs) throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        for (String addr : addrs) {
            writeUint(stream, addr.length());
            stream.write(addr.getBytes(ENC));
        }
        return stream.toByteArray();
    }

    /**
     * Decodes addresses from a byte array that was encoded by packAddresses
     *
     * @param input         the byte array to decode
     * @return              the list of addresses.
     * @throws IOException  if the addresses can't be decoded
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
            output.add(new String(data, ENC));
        }
        return output;
    }

    /**
     * Encodes the encryption algorithm and keys passed in.
     *
     * @param algo          the encryption algorithm to use; See
     *                      {@link io.v.x.ref.lib.discovery.Constants} for valid values
     * @param keys          the keys to encode
     * @return              the byte array that is the encoded form
     * @throws IOException  if the keys can't be encoded
     */
    public static byte[] packEncryptionKeys(EncryptionAlgorithm algo, List<EncryptionKey> keys)
            throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        writeUint(stream, algo.getValue());
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
     * @param input         the byte array to decode
     * @param keys          the keys where the decoded keys is stored
     * @return              the encryption algorithm
     * @throws IOException  if the keys can't be decoded
     */
    public static EncryptionAlgorithm unpackEncryptionKeys(byte[] input, List<EncryptionKey> keys)
            throws IOException {
        ByteArrayInputStream stream = new ByteArrayInputStream(input);
        int algo = readUint(stream);
        while (stream.available() > 0) {
            int size = readUint(stream);
            byte[] key = new byte[size];
            int read = stream.read(key);
            if (read != size) {
                throw new EOFException();
            }
            keys.add(new EncryptionKey(Bytes.asList(key)));
        }
        return new EncryptionAlgorithm(algo);
    }
}
