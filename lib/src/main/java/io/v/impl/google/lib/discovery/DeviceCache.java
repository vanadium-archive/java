// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.impl.google.lib.discovery;


import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;

import org.joda.time.Duration;
import org.joda.time.Instant;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.v.x.ref.lib.discovery.Advertisement;

/**
 * A cache of ble devices that were seen recently.  The current Vanadium BLE protocol requires
 * connecting to the advertiser to grab the attributes and the addrs.  This can be expensive
 * so we only refetch the data if it hash changed.
 */
public class DeviceCache {
    final private Map<Long, CacheEntry> cachedDevices = new HashMap<>();
    final private Map<String, CacheEntry> knownIds = new HashMap<>();


    final AtomicInteger nextScanner = new AtomicInteger(0);
    final private SetMultimap<UUID, Advertisement> knownServices = HashMultimap.create();
    final private Map<Integer, VScanner> scannersById = new HashMap<>();
    final private SetMultimap<UUID, VScanner> scannersByUUID = HashMultimap.create();
    ScheduledExecutorService timer;


    final private Duration maxAge;


    public DeviceCache(final Duration maxAge) {
        this.maxAge = maxAge;
        this.timer = Executors.newSingleThreadScheduledExecutor();
        long periodicity = maxAge.getMillis() / 2;
        timer.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                removeStaleEntries();
            }
        }, periodicity, periodicity, TimeUnit.MILLISECONDS);
    }

    void removeStaleEntries() {
        synchronized (this) {
            Iterator<Map.Entry<Long, CacheEntry>> it = cachedDevices.entrySet().iterator();

            while (it.hasNext()) {
                Map.Entry<Long, CacheEntry> mapEntry = it.next();
                CacheEntry entry = mapEntry.getValue();
                if (entry.lastSeen.plus(maxAge).isBeforeNow()) {
                    it.remove();
                    knownIds.remove(entry.deviceId);
                    for (Advertisement adv : entry.advertisements) {
                        UUID uuid = UUIDUtil.UuidToUUID(adv.getServiceUuid());
                        knownServices.remove(uuid, adv);
                        adv.setLost(true);
                        handleUpdate(adv);
                    }
                }
            }
        }
    }


    /**
     * Cleans up the cache's state and shutdowns the eviction thread.
     */
    public void shutdownCache() {
        timer.shutdown();
    }

    /**
     * Returns whether this hash has been seen before.
     *
     * @param hash the hash of the advertisement
     * @param deviceId the deviceId of the advertisement (used to handle rotating ids).
     * @return true iff this hash is in the cache.
     */
    public boolean haveSeenHash(long hash, String deviceId) {
        synchronized (this) {
            CacheEntry entry = cachedDevices.get(hash);
            if (entry != null) {
                entry.lastSeen = new Instant();
                if (!entry.deviceId.equals(deviceId)) {
                    // This probably happened becuase a device has changed it's ble mac address.
                    // We need to update the mac address for this entry.
                    knownIds.remove(entry.deviceId);
                    entry.deviceId = deviceId;
                    knownIds.put(deviceId, entry);
                }
            }
            return entry != null;
        }
    }

    /**
     * Saves the set of advertisements for this deviceId and hash
     *
     * @param hash the hash provided by the advertisement.
     * @param advs the advertisements exposed by the device.
     * @param deviceId the id of the device.
     */
    public void saveDevice(long hash, Set<Advertisement> advs, String deviceId) {
        CacheEntry entry = new CacheEntry(advs, hash, deviceId);
        synchronized (this) {
            CacheEntry oldEntry = knownIds.get(deviceId);
            Set<Advertisement> oldValues = null;
            if (oldEntry != null) {
                cachedDevices.remove(oldEntry.hash);
                knownIds.remove(oldEntry.deviceId);
                oldValues = oldEntry.advertisements;
            } else {
                oldValues = new HashSet<>();
            }
            Set<Advertisement> removed = Sets.difference(oldValues, advs);
            for (Advertisement adv : removed) {
                UUID uuid = UUIDUtil.UuidToUUID(adv.getServiceUuid());
                adv.setLost(true);
                knownServices.remove(uuid, adv);
                handleUpdate(adv);
            }

            Set<Advertisement> added = Sets.difference(advs, oldValues);
            for (Advertisement adv: added) {
                UUID uuid = UUIDUtil.UuidToUUID(adv.getServiceUuid());
                knownServices.put(uuid, adv);
                handleUpdate(adv);
            }
            cachedDevices.put(hash, entry);
            CacheEntry oldDeviceEntry = knownIds.get(deviceId);
            if (oldDeviceEntry != null) {
                // Delete the old hash value.
                cachedDevices.remove(hash);
            }
            knownIds.put(deviceId, entry);
        }
    }

    private void handleUpdate(Advertisement adv) {
        UUID uuid = UUIDUtil.UuidToUUID(adv.getServiceUuid());
        Set<VScanner> scanners = scannersByUUID.get(uuid);
        if (scanners == null) {
            return;
        }
        for (VScanner scanner : scanners) {
            scanner.getHandler().handleUpdate(adv);
        }
    }

    /**
     * Adds a scanner that will be notified when advertisements that match its query have changed.
     *
     * @return the handle of the scanner that can be used to remove the scanner.
     */
    public int addScanner(VScanner scanner) {
        synchronized (this) {
            int id = nextScanner.addAndGet(1);
            scannersById.put(id, scanner);
            scannersByUUID.put(scanner.getServiceUUID(), scanner);
            Set<Advertisement> knownAdvs = knownServices.get(scanner.getServiceUUID());
            if (knownAdvs != null) {
                for (Advertisement adv : knownAdvs) {
                    scanner.getHandler().handleUpdate(adv);
                }
            }
            return id;
        }
    }

    /**
     * Removes the scanner matching this id.  This scanner will stop getting updates.
     */
    public void removeScanner(int id) {
        synchronized (this) {
            VScanner scanner = scannersById.get(id);
            if (scanner != null) {
                scannersByUUID.remove(scanner.getServiceUUID(), scanner);
                scannersById.remove(id);
            }
        }
    }

    private class CacheEntry {
        Set<Advertisement> advertisements;

        long hash;

        Instant lastSeen;

        String deviceId;

        CacheEntry(Set<Advertisement> advs, long hash, String deviceId) {
            advertisements = advs;
            this.hash = hash;
            lastSeen = new Instant();
            this.deviceId = deviceId;
        }
    }

}
