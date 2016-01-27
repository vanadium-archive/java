// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.impl.google.lib.discovery.ble;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;
import java.util.Map;
import java.util.UUID;

import io.v.impl.google.lib.discovery.EncodingUtil;
import io.v.impl.google.lib.discovery.UUIDUtil;
import io.v.x.ref.lib.discovery.Advertisement;
import io.v.v23.discovery.Attachments;
import io.v.v23.discovery.Attributes;
import io.v.v23.discovery.Service;
import io.v.x.ref.lib.discovery.EncryptionAlgorithm;
import io.v.x.ref.lib.discovery.EncryptionKey;
import io.v.x.ref.lib.discovery.plugins.ble.Constants;

/**
 * Converts from {@link Advertisement} to the gatt services and vice-versa.
 */
public class BleAdvertisementConverter {
    private static final Logger logger = Logger.getLogger(BleAdvertisementConverter.class.getName());
    private static final Charset enc = Charset.forName("UTF-8");

    /**
     * Converts from {@link Advertisement} to the ble representation.
     *
     * @return map of Characteristic UUIDs to their values.
     * @throws IOException
     */
    public static Map<UUID, byte[]> vAdvertismentToBleAttr(Advertisement adv)
            throws IOException {
        Map<UUID, byte[]> bleAttr = new HashMap<>();
        Service service = adv.getService();
        bleAttr.put(UUID.fromString(Constants.INSTANCE_ID_UUID),
                    service.getInstanceId().getBytes(enc));
        bleAttr.put(UUID.fromString(Constants.INTERFACE_NAME_UUID),
                    service.getInterfaceName().getBytes(enc));
        bleAttr.put(UUID.fromString(Constants.ADDRS_UUID),
                    EncodingUtil.packAddresses(service.getAddrs()));
        bleAttr.put(UUID.fromString(Constants.HASH_UUID), adv.getHash());

        String instanceName = service.getInstanceName();
        if (instanceName != null && !instanceName.isEmpty()) {
            bleAttr.put(UUID.fromString(Constants.INSTANCE_NAME_UUID),
                        instanceName.getBytes(enc));
        }
        for (Map.Entry<String, String> entry : service.getAttrs().entrySet()) {
            String key = entry.getKey();
            String data = key + "=" + entry.getValue();
            bleAttr.put(UUIDUtil.UUIDForAttributeKey(key), data.getBytes(enc));
        }
        for (Map.Entry<String, byte[]> entry : service.getAttachments().entrySet()) {
            String key = Constants.ATTACHMENT_NAME_PREFIX + entry.getKey();
            byte[] keyInBytes = key.getBytes(enc);
            byte[] value = entry.getValue();
            ByteArrayOutputStream buf =
                new ByteArrayOutputStream(keyInBytes.length + 1 + value.length);
            buf.write(keyInBytes);
            buf.write((byte)'=');
            buf.write(value);
            bleAttr.put(UUIDUtil.UUIDForAttributeKey(key), buf.toByteArray());
        }
        if (adv.getEncryptionAlgorithm().getValue() != 0) {
            bleAttr.put(UUID.fromString(Constants.ENCRYPTION_UUID),
                        EncodingUtil.packEncryptionKeys(adv.getEncryptionAlgorithm().getValue(),
                                                        adv.getEncryptionKeys()));
        }
        List<String> dirAddrs = adv.getDirAddrs();
        if (dirAddrs != null && !dirAddrs.isEmpty()) {
            bleAttr.put(UUID.fromString(Constants.DIR_ADDRS_UUID),
                        EncodingUtil.packAddresses(dirAddrs));
        }
        return bleAttr;
    }

    /**
     * Converts from Map of Characteristic UUIDs -> values to a {@link Advertisement}
     *
     * @param attr the map of characteristic uuids to their values
     * @return the Vanadium Advertisement based on characteristics.
     * @throws IOException
     */
    public static Advertisement bleAttrToVAdvertisement(Map<UUID, byte[]> bleAttr)
            throws IOException {
        String instanceId = null;
        String instanceName = null;
        String interfaceName = null;
        List<String> addrs = null;
        Map<String, String> attrs = new HashMap<String, String>();
        Map<String, byte[]> attachments = new HashMap<String, byte[]>();
        int encryptionAlgo = 0;
        List<EncryptionKey> encryptionKeys = null;
        byte[] hash = null;
        List<String> dirAddrs = null;

        for (Map.Entry<UUID, byte[]> entry : bleAttr.entrySet()) {
            String uuidKey = entry.getKey().toString();
            byte[] data = entry.getValue();
            if (uuidKey.equals(Constants.INSTANCE_ID_UUID)) {
                instanceId = new String(data, enc);
            } else if (uuidKey.equals(Constants.INSTANCE_NAME_UUID)) {
                instanceName = new String(data, enc);
            } else if (uuidKey.equals(Constants.INTERFACE_NAME_UUID)) {
                interfaceName = new String(data, enc);
            } else if (uuidKey.equals(Constants.ADDRS_UUID)) {
                addrs = EncodingUtil.unpackAddresses(data);
            } else if (uuidKey.equals(Constants.ENCRYPTION_UUID)) {
                EncodingUtil.KeysAndAlgorithm res = EncodingUtil.unpackEncryptionKeys(data);
                encryptionAlgo = res.getEncryptionAlgorithm();
                encryptionKeys = res.getKeys();
            } else if (uuidKey.equals(Constants.HASH_UUID)) {
                hash = data;
            } else if (uuidKey.equals(Constants.DIR_ADDRS_UUID)) {
                dirAddrs = EncodingUtil.unpackAddresses(data);
            } else {
                int index = -1;
                for (int i = 0; i < data.length; i++) {
                  if (data[i] == (byte)'=') {
                    index = i;
                    break;
                  }
                }
                if (index < 0) {
                    logger.severe("Failed to parse data for " + uuidKey);
                    continue;
                }
                String key = new String(data, 0, index, enc);
                if (key.startsWith(Constants.ATTACHMENT_NAME_PREFIX)) {
                  key = key.substring(Constants.ATTACHMENT_NAME_PREFIX.length());
                  byte[] value = Arrays.copyOfRange(data, index + 1, data.length);
                  attachments.put(key, value);
                } else {
                  String value = new String(data, index + 1, data.length - index - 1, enc);
                  attrs.put(key, value);
                }
            }
        }
        return new Advertisement(
                new Service(instanceId,
                            instanceName,
                            interfaceName,
                            new Attributes(attrs),
                            addrs,
                            new Attachments(attachments)),
                new EncryptionAlgorithm(encryptionAlgo),
                encryptionKeys,
                hash,
                dirAddrs,
                false);
    }
}
