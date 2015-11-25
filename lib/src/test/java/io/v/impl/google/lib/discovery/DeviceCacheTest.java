// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.impl.google.lib.discovery;

import io.v.v23.discovery.Service;
import io.v.x.ref.lib.discovery.Advertisement;
import io.v.x.ref.lib.discovery.EncryptionAlgorithm;
import io.v.x.ref.lib.discovery.Uuid;
import junit.framework.TestCase;
import org.joda.time.DateTimeUtils;
import org.joda.time.Duration;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Tests for {@link DeviceCache}.
 */
public class DeviceCacheTest extends TestCase {
    private abstract class CountingHandler implements ScanHandler {
        protected int mNumCalls = 0;

        @Override
        public void handleUpdate(Advertisement adv) {}
    }

    public void tearDown() {
        DateTimeUtils.setCurrentMillisSystem();
    }

    public void testSaveDeveice() {
        DeviceCache cache = new DeviceCache(new Duration(1000 * 60 * 60));
        // The advertisements here are not relevant since we are just checking
        // the seen hash function.
        Set<Advertisement> advs = new HashSet<>();
        long hash = 10001;
        assertFalse(cache.haveSeenHash(hash, "newDevice"));
        cache.saveDevice(hash, advs, "newDevice");
        assertTrue(cache.haveSeenHash(hash, "newDevice"));
        cache.shutdownCache();
    }

    public void testSaveDeviceWithDifferentHashCode() {
        DeviceCache cache = new DeviceCache(new Duration(1000 * 60 * 60));
        // The advertisements here are not relevant since we are just checking
        // the seen hash function.
        Set<Advertisement> advs = new HashSet<>();
        long hash = 10001;
        assertFalse(cache.haveSeenHash(hash, "newDevice"));
        cache.saveDevice(hash, advs, "newDevice");
        assertTrue(cache.haveSeenHash(hash, "newDevice"));
        cache.saveDevice(hash + 1, advs, "newDevice");
        assertTrue(cache.haveSeenHash(hash + 1, "newDevice"));
        assertFalse(cache.haveSeenHash(hash, "newDevice"));
        cache.shutdownCache();
    }

    public void testAddingScannerBeforeSavingDevice() {
        DeviceCache cache = new DeviceCache(new Duration(1000 * 60 * 60));
        Set<Advertisement> advs = new HashSet<>();
        long hash = 10001;
        assertFalse(cache.haveSeenHash(hash, "newDevice"));

        Service service1 = new Service();
        service1.setInterfaceName("randomInterface");
        Uuid uuid1 = UUIDUtil.UUIDToUuid(UUID.randomUUID());
        final Advertisement adv1 = new Advertisement(service1, uuid1, new EncryptionAlgorithm(0), null, false);
        advs.add(adv1);

        CountingHandler handler = new CountingHandler() {
            @Override
            public void handleUpdate(Advertisement advertisement) {
                assertEquals(adv1, advertisement);
                assertEquals(mNumCalls, 0);
                mNumCalls++;
            }
        };

        cache.addScanner(new VScanner(UUIDUtil.UuidToUUID(uuid1), handler));

        Service service2 = new Service();
        service1.setInterfaceName("randomInterface2");
        Uuid uuid2 = UUIDUtil.UUIDToUuid(UUID.randomUUID());
        Advertisement adv2 = new Advertisement(service2, uuid2, new EncryptionAlgorithm(0), null, false);
        advs.add(adv2);

        cache.saveDevice(hash, advs, "newDevice");

        // Make sure that the handler is called;
        assertEquals(1, handler.mNumCalls);
        cache.shutdownCache();
    }

    public void testAddingScannerAfterSavingDevice() {
        DeviceCache cache = new DeviceCache(new Duration(1000 * 60 * 60));
        Set<Advertisement> advs = new HashSet<>();
        long hash = 10001;
        assertFalse(cache.haveSeenHash(hash, "newDevice"));

        Service service1 = new Service();
        service1.setInterfaceName("randomInterface");
        Uuid uuid1 = UUIDUtil.UUIDToUuid(UUID.randomUUID());
        final Advertisement adv1 = new Advertisement(service1, uuid1, new EncryptionAlgorithm(0), null, false);

        advs.add(adv1);

        CountingHandler handler = new CountingHandler() {
            @Override
            public void handleUpdate(Advertisement advertisement) {
                assertEquals(adv1, advertisement);
                assertEquals(mNumCalls, 0);
                mNumCalls++;
            }
        };

        Service service2 = new Service();
        service1.setInterfaceName("randomInterface2");
        Uuid uuid2 = UUIDUtil.UUIDToUuid(UUID.randomUUID());
        Advertisement adv2 = new Advertisement(service2, uuid2, new EncryptionAlgorithm(0), null, false);
        advs.add(adv2);
        cache.saveDevice(hash, advs, "newDevice");

        cache.addScanner(new VScanner(UUIDUtil.UuidToUUID(uuid1), handler));

        // Make sure that the handler is called;
        assertEquals(1, handler.mNumCalls);
        cache.shutdownCache();
    }

    public void testRemovingAnAdvertisementCallsHandler() {
        DeviceCache cache = new DeviceCache(new Duration(1000 * 60 * 60));
        Set<Advertisement> advs = new HashSet<>();
        long hash = 10001;
        assertFalse(cache.haveSeenHash(hash, "newDevice"));

        Service service1 = new Service();
        service1.setInterfaceName("randomInterface");
        Uuid uuid1 = UUIDUtil.UUIDToUuid(UUID.randomUUID());
        final Advertisement adv1 = new Advertisement(service1, uuid1, new EncryptionAlgorithm(0), null, false);
        advs.add(adv1);

        CountingHandler handler = new CountingHandler() {
            @Override
            public void handleUpdate(Advertisement advertisement) {
                // The first call should be an add and the second call should be
                // a remove.
                if (mNumCalls == 0) {
                    assertEquals(adv1, advertisement);
                } else {
                    Advertisement removed = new Advertisement(
                            adv1.getService(), adv1.getServiceUuid(),
                            adv1.getEncryptionAlgorithm(), adv1.getEncryptionKeys(), true);
                    assertEquals(removed, advertisement);
                }
                mNumCalls++;
            }
        };

        cache.addScanner(new VScanner(UUIDUtil.UuidToUUID(uuid1), handler));

        Service service2 = new Service();
        service2.setInterfaceName("randomInterface2");
        Uuid uuid2 = UUIDUtil.UUIDToUuid(UUID.randomUUID());
        Advertisement adv2 = new Advertisement(service2, uuid2, new EncryptionAlgorithm(0), null, false);
        advs.add(adv2);

        cache.saveDevice(hash, advs, "newDevice");

        Set<Advertisement> newAdvs = new HashSet<>();
        newAdvs.add(adv2);

        cache.saveDevice(10002, newAdvs, "newDevice");

        // Make sure that the handler is called;
        assertEquals(2, handler.mNumCalls);
        cache.shutdownCache();
    }

    public void testAddingtheSameAdvertisementDoesNotCallsHandler() {
        DeviceCache cache = new DeviceCache(new Duration(1000 * 60 * 60));
        Set<Advertisement> advs = new HashSet<>();
        long hash = 10001;
        assertFalse(cache.haveSeenHash(hash, "newDevice"));

        Service service1 = new Service();
        service1.setInterfaceName("randomInterface");
        Uuid uuid1 = UUIDUtil.UUIDToUuid(UUID.randomUUID());
        final Advertisement adv1 = new Advertisement(service1, uuid1, new EncryptionAlgorithm(0), null, false);
        advs.add(adv1);

        CountingHandler handler = new CountingHandler() {
            @Override
            public void handleUpdate(Advertisement advertisement) {
                assertEquals(adv1, advertisement);
                mNumCalls++;
            }
        };

        cache.addScanner(new VScanner(UUIDUtil.UuidToUUID(uuid1), handler));

        Service service2 = new Service();
        service2.setInterfaceName("randomInterface2");
        Uuid uuid2 = UUIDUtil.UUIDToUuid(UUID.randomUUID());
        Advertisement adv2 = new Advertisement(service2, uuid2, new EncryptionAlgorithm(0), null, false);
        advs.add(adv2);

        cache.saveDevice(hash, advs, "newDevice");

        Set<Advertisement> advs2 = new HashSet<>(advs);
        cache.saveDevice(10002, advs2, "newDevice");

        // Make sure that the handler is called;
        assertEquals(1, handler.mNumCalls);
        cache.shutdownCache();
    }

    public void testCacheEvictionCallsHandler() {
        DeviceCache cache = new DeviceCache(new Duration(1000 * 60 * 60));
        Set<Advertisement> advs = new HashSet<>();
        long hash = 10001;
        assertFalse(cache.haveSeenHash(hash, "newDevice"));

        Service service1 = new Service();
        service1.setInterfaceName("randomInterface");
        Uuid uuid1 = UUIDUtil.UUIDToUuid(UUID.randomUUID());
        final Advertisement adv1 = new Advertisement(service1, uuid1, new EncryptionAlgorithm(0), null, false);
        advs.add(adv1);

        CountingHandler handler = new CountingHandler() {
            @Override
            public void handleUpdate(Advertisement advertisement) {
                // The first call should be an add and the second call should be
                // a remove.
                if (mNumCalls == 0) {
                    assertEquals(adv1, advertisement);
                } else {
                    Advertisement removed = new Advertisement(
                            adv1.getService(), adv1.getServiceUuid(),
                            adv1.getEncryptionAlgorithm(), adv1.getEncryptionKeys(), true);
                    assertEquals(removed, advertisement);
                }
                mNumCalls++;
            }
        };

        long cacheTime = DateTimeUtils.currentTimeMillis();
        cache.saveDevice(hash, advs, "newDevice");
        cache.addScanner(new VScanner(UUIDUtil.UuidToUUID(uuid1), handler));

        DateTimeUtils.setCurrentMillisFixed(cacheTime + 1000 * 60 * 61);
        cache.removeStaleEntries();
        // Make sure that the handler is called;
        assertEquals(2, handler.mNumCalls);
        cache.shutdownCache();
    }

    public void testCacheEvictionClearsAllState() {
        DeviceCache cache = new DeviceCache(new Duration(1000 * 60 * 60));
        Set<Advertisement> advs = new HashSet<>();
        long hash = 10001;
        assertFalse(cache.haveSeenHash(hash, "newDevice"));

        Service service1 = new Service();
        service1.setInterfaceName("randomInterface");
        Uuid uuid1 = UUIDUtil.UUIDToUuid(UUID.randomUUID());
        final Advertisement adv1 = new Advertisement(service1, uuid1, new EncryptionAlgorithm(0), null, false);
        advs.add(adv1);

        CountingHandler handler = new CountingHandler() {
            @Override
            public void handleUpdate(Advertisement advertisement) {
               mNumCalls++;
            }
        };

        long cacheTime = DateTimeUtils.currentTimeMillis();
        cache.saveDevice(hash, advs, "newDevice");

        DateTimeUtils.setCurrentMillisFixed(cacheTime + 1000 * 60 * 61);
        cache.removeStaleEntries();

        cache.addScanner(new VScanner(UUIDUtil.UuidToUUID(uuid1), handler));

        // Make sure that the handler is never called.
        assertEquals(0, handler.mNumCalls);
        cache.shutdownCache();
    }
}
