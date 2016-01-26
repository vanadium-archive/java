// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.impl.google.lib.discovery;


import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;

import org.joda.time.Duration;
import org.joda.time.Instant;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.v.x.ref.lib.discovery.Advertisement;

/**
 * A cache of ble devices that were seen recently.  The current Vanadium BLE protocol requires
 * connecting to the advertiser to grab the attributes and the addrs.  This can be expensive
 * so we only refetch the data if its stamp changed.
 */
public class DeviceCache {
    private final Map<Long, CacheEntry> cachedDevices = new HashMap<>();
    private final Map<String, CacheEntry> knownIds = new HashMap<>();

    private final AtomicInteger nextScanner = new AtomicInteger(0);
    private final SetMultimap<String, Advertisement> knownServices = HashMultimap.create();
    private final Map<Integer, VScanner> scannersById = new HashMap<>();
    private final SetMultimap<String, VScanner> scannersByInterfaceName = HashMultimap.create();
    ScheduledExecutorService timer;

    private final Duration maxAge;


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
                        knownServices.remove(adv.getService().getInterfaceName(), adv);
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
     * Returns whether this stamp has been seen before.
     *
     * @param stamp the stamp of the advertisement
     * @param deviceId the deviceId of the advertisement (used to handle rotating ids).
     * @return true iff this stamp is in the cache.
     */
    public boolean haveSeenStamp(long stamp, String deviceId) {
        synchronized (this) {
            CacheEntry entry = cachedDevices.get(stamp);
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
     * Saves the set of advertisements and stamp for this device.
     *
     * @param stamp the stamp provided by the device.
     * @param advs the advertisements exposed by the device.
     * @param deviceId the id of the device.
     */
    public void saveDevice(long stamp, Set<Advertisement> advs, String deviceId) {
        CacheEntry entry = new CacheEntry(advs, stamp, deviceId);
        synchronized (this) {
            CacheEntry oldEntry = knownIds.get(deviceId);
            Set<Advertisement> oldValues = null;
            if (oldEntry != null) {
                cachedDevices.remove(oldEntry.stamp);
                knownIds.remove(oldEntry.deviceId);
                oldValues = oldEntry.advertisements;
            } else {
                oldValues = new HashSet<>();
            }
            Set<Advertisement> removed = Sets.difference(oldValues, advs);
            for (Advertisement adv : removed) {
                knownServices.remove(adv.getService().getInterfaceName(), adv);
                adv.setLost(true);
                handleUpdate(adv);
            }

            Set<Advertisement> added = Sets.difference(advs, oldValues);
            for (Advertisement adv: added) {
                knownServices.put(adv.getService().getInterfaceName(), adv);
                handleUpdate(adv);
            }
            cachedDevices.put(stamp, entry);
            CacheEntry oldDeviceEntry = knownIds.get(deviceId);
            if (oldDeviceEntry != null) {
                // Delete the old stamp value.
                cachedDevices.remove(stamp);
            }
            knownIds.put(deviceId, entry);
        }
    }

    private void handleUpdate(Advertisement adv) {
        Set<VScanner> scanners = scannersByInterfaceName.get(adv.getService().getInterfaceName());
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
            scannersByInterfaceName.put(scanner.getInterfaceName(), scanner);
            Set<Advertisement> knownAdvs = knownServices.get(scanner.getInterfaceName());
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
                scannersByInterfaceName.remove(scanner.getInterfaceName(), scanner);
                scannersById.remove(id);
            }
        }
    }

    private class CacheEntry {
        Set<Advertisement> advertisements;

        long stamp;

        Instant lastSeen;

        String deviceId;

        CacheEntry(Set<Advertisement> advs, long stamp, String deviceId) {
            advertisements = advs;
            this.stamp = stamp;
            lastSeen = new Instant();
            this.deviceId = deviceId;
        }
    }

}
