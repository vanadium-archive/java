// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.impl.google.lib.discovery.ble;


import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
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
 *
 * TODO(jhahn): Handle Attachments.
 */
public class BleAdvertisementConverter {
    private static Charset enc = Charset.forName("UTF-8");

    /**
     * Converts from {@link Advertisement} to the ble representation.
     *
     * @return map of Characteristic UUIDs to their values.
     * @throws IOException
     */
    public static Map<UUID, byte[]> vAdvertismentToBleAttr(Advertisement adv)
            throws IOException {
        Map<UUID, byte[]> attr = new HashMap<>();
        Service service = adv.getService();
        attr.put(UUID.fromString(Constants.INSTANCE_ID_UUID),
                service.getInstanceId().getBytes(enc));
        attr.put(UUID.fromString(Constants.INTERFACE_NAME_UUID),
                service.getInterfaceName().getBytes(enc));
        attr.put(UUID.fromString(Constants.ADDRS_UUID),
                EncodingUtil.packAddresses(service.getAddrs()));

        String instanceName = service.getInstanceName();
        if (instanceName != null && !instanceName.isEmpty()) {
            attr.put(UUID.fromString(Constants.INSTANCE_NAME_UUID), instanceName.getBytes(enc));
        }

        if (adv.getEncryptionAlgorithm().getValue() != 0) {
            attr.put(UUID.fromString(Constants.ENCRYPTION_UUID),
                    EncodingUtil.packEncryptionKeys(adv.getEncryptionAlgorithm().getValue(),
                            adv.getEncryptionKeys()));
        }

        for (Map.Entry<String, String> keyAndValue : service.getAttrs().entrySet()) {
            String key = keyAndValue.getKey();
            UUID attrKey = UUIDUtil.UUIDForAttributeKey(key);
            String attrValue = key + "=" + keyAndValue.getValue();
            attr.put(attrKey, attrValue.getBytes(enc));
        }
        return attr;
    }

    /**
     * Converts from Map of Characteristic UUIDs -> values to a {@link Advertisement}
     *
     * @param attr the map of characteristic uuids to their values
     * @return the Vanadium Advertisement based on characteristics.
     * @throws IOException
     */
    public static Advertisement bleAttrToVAdvertisement(Map<UUID, byte[]> attr)
            throws IOException {
        Map<String, String> cleanAttrs = new HashMap<String, String>();
        String instanceId = null;
        String interfaceName = null;
        String instanceName = null;
        List<String> addrs = null;
        int encryptionAlgo = 0;
        List<EncryptionKey> encryptionKeys = null;

        for (Map.Entry<UUID, byte[]> entry : attr.entrySet()) {
            String uuidKey = entry.getKey().toString();
            System.out.println("key is " + uuidKey);
            byte[] value = entry.getValue();
            if (uuidKey.equals(Constants.INSTANCE_ID_UUID)) {
                instanceId = new String(value, enc);
            } else if (uuidKey.equals(Constants.INSTANCE_NAME_UUID)) {
                instanceName = new String(value, enc);
            } else if (uuidKey.equals(Constants.INTERFACE_NAME_UUID)) {
                interfaceName = new String(value, enc);
            } else if (uuidKey.equals(Constants.ADDRS_UUID)) {
                addrs = EncodingUtil.unpackAddresses(value);
            } else if (uuidKey.equals(Constants.ENCRYPTION_UUID)) {
                EncodingUtil.KeysAndAlgorithm res = EncodingUtil.unpackEncryptionKeys(value);
                encryptionAlgo = res.getEncryptionAlgorithm();
                encryptionKeys = res.getKeys();
            } else {
                String keyAndValue = new String(value, enc);
                String[] parts = keyAndValue.split("=", 2);
                if (parts.length != 2) {
                    System.err.println("Failed to parse key and value" + keyAndValue);
                    continue;
                }
                cleanAttrs.put(parts[0], parts[1]);
            }
        }
        return new Advertisement(
                new Service(instanceId, instanceName, interfaceName, new Attributes(cleanAttrs), addrs, new Attachments()),
                UUIDUtil.UUIDToUuid(UUIDUtil.UUIDForInterfaceName(interfaceName)),
                new EncryptionAlgorithm(encryptionAlgo),
                encryptionKeys,
                false);
    }
}
