// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.impl.google.discovery.plugins.ble;

import com.google.common.primitives.Bytes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import io.v.v23.discovery.Advertisement;
import io.v.v23.discovery.AdId;

import io.v.x.ref.lib.discovery.AdInfo;
import io.v.x.ref.lib.discovery.AdHash;
import io.v.x.ref.lib.discovery.EncryptionAlgorithm;
import io.v.x.ref.lib.discovery.EncryptionKey;
import io.v.x.ref.lib.discovery.plugins.ble.Constants;

import io.v.impl.google.lib.discovery.EncodingUtil;
import io.v.impl.google.lib.discovery.UUIDUtil;

/**
 * Converts from {@link AdInfo} to GATT characteristics and vice-versa.
 */
class ConvertUtil {
    private static final Logger logger = Logger.getLogger(ConvertUtil.class.getName());

    // We use "ISO8859-1" to preserve data in a string without any interpretation.
    private static final Charset ENC = Charset.forName("ISO8859-1");

    private static final UUID UUID_ID = UUID.fromString(Constants.ID_UUID);
    private static final UUID UUID_INTERFACE_NAME = UUID.fromString(Constants.INTERFACE_NAME_UUID);
    private static final UUID UUID_ADDRESSES = UUID.fromString(Constants.ADDRESSES_UUID);
    private static final UUID UUID_ENCRYPTION = UUID.fromString(Constants.ENCRYPTION_UUID);
    private static final UUID UUID_HASH = UUID.fromString(Constants.HASH_UUID);
    private static final UUID UUID_DIR_ADDRS = UUID.fromString(Constants.DIR_ADDRS_UUID);

    /**
     * Converts from {@link AdInfo} to GATT characteristics.
     *
     * @param adinfo        an advertisement information to convert
     * @return              a map of GATT characteristics corresponding to the {@link adinfo}
     * @throws IOException  if the advertisement can't be converted
     */
    static Map<UUID, byte[]> toGattAttrs(AdInfo adinfo) throws IOException {
        Map<UUID, byte[]> gatt = new HashMap<>();
        Advertisement ad = adinfo.getAd();
        gatt.put(UUID_ID, ad.getId().toPrimitiveArray());
        gatt.put(UUID_INTERFACE_NAME, ad.getInterfaceName().getBytes(ENC));
        gatt.put(UUID_ADDRESSES, EncodingUtil.packAddresses(ad.getAddresses()));

        Map<String, String> attributes = ad.getAttributes();
        if (attributes != null && attributes.size() > 0) {
            for (Map.Entry<String, String> entry : attributes.entrySet()) {
                String key = entry.getKey();
                String data = key + "=" + entry.getValue();
                gatt.put(UUIDUtil.attributeUUID(key), data.getBytes(ENC));
            }
        }

        Map<String, byte[]> attachments = ad.getAttachments();
        if (attachments != null && attachments.size() > 0) {
            for (Map.Entry<String, byte[]> entry : attachments.entrySet()) {
                String key = Constants.ATTACHMENT_NAME_PREFIX + entry.getKey();
                ByteArrayOutputStream buf = new ByteArrayOutputStream();
                buf.write(key.getBytes(ENC));
                buf.write((byte) '=');
                buf.write(entry.getValue());
                gatt.put(UUIDUtil.attributeUUID(key), buf.toByteArray());
            }
        }
        if (adinfo.getEncryptionAlgorithm() != io.v.x.ref.lib.discovery.Constants.NO_ENCRYPTION) {
            gatt.put(
                    UUID_ENCRYPTION,
                    EncodingUtil.packEncryptionKeys(
                            adinfo.getEncryptionAlgorithm(), adinfo.getEncryptionKeys()));
        }
        List<String> dirAddrs = adinfo.getDirAddrs();
        if (dirAddrs != null && !dirAddrs.isEmpty()) {
            gatt.put(UUID_DIR_ADDRS, EncodingUtil.packAddresses(dirAddrs));
        }
        gatt.put(UUID_HASH, adinfo.getHash().toPrimitiveArray());
        return gatt;
    }

    /**
     * Converts from GATT characteristics to {@link AdInfo}.
     *
     * @param attrs         a map of GATT characteristics
     * @return              an advertisement information corresponding to the {@link attrs}
     * @throws IOException  if the GATT characteristics can't be converted
     */
    static AdInfo toAdInfo(Map<UUID, byte[]> attrs) throws IOException {
        AdInfo adinfo = new AdInfo();
        Advertisement ad = adinfo.getAd();
        for (Map.Entry<UUID, byte[]> entry : attrs.entrySet()) {
            UUID uuid = entry.getKey();
            byte[] data = entry.getValue();

            if (uuid.equals(UUID_ID)) {
                ad.setId(new AdId(data));
            } else if (uuid.equals(UUID_INTERFACE_NAME)) {
                ad.setInterfaceName(new String(data, ENC));
            } else if (uuid.equals(UUID_ADDRESSES)) {
                ad.setAddresses(EncodingUtil.unpackAddresses(data));
            } else if (uuid.equals(UUID_ENCRYPTION)) {
                List<EncryptionKey> keys = new ArrayList<>();
                EncryptionAlgorithm algo = EncodingUtil.unpackEncryptionKeys(data, keys);
                adinfo.setEncryptionAlgorithm(algo);
                adinfo.setEncryptionKeys(keys);
            } else if (uuid.equals(UUID_DIR_ADDRS)) {
                adinfo.setDirAddrs(EncodingUtil.unpackAddresses(data));
            } else if (uuid.equals(UUID_HASH)) {
                adinfo.setHash(new AdHash(data));
            } else {
                int index = Bytes.indexOf(data, (byte) '=');
                if (index < 0) {
                    logger.severe("Failed to parse data for " + uuid);
                    continue;
                }
                String key = new String(data, 0, index, ENC);
                if (key.startsWith(Constants.ATTACHMENT_NAME_PREFIX)) {
                    key = key.substring(Constants.ATTACHMENT_NAME_PREFIX.length());
                    byte[] value = Arrays.copyOfRange(data, index + 1, data.length);
                    ad.getAttachments().put(key, value);
                } else {
                    String value = new String(data, index + 1, data.length - index - 1, ENC);
                    ad.getAttributes().put(key, value);
                }
            }
        }
        return adinfo;
    }
}
