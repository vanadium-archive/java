// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.impl.google.discovery.plugins.ble;

import com.google.common.base.Equivalence;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;

import org.joda.time.Duration;
import org.joda.time.Instant;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import io.v.x.ref.lib.discovery.AdInfo;

import io.v.impl.google.lib.discovery.Plugin;

/**
 * A cache of ble devices that were seen recently.
 * <p>
 * The current Vanadium BLE protocol requires connecting to the advertiser
 * to grab the attributes and the addrs. This can be expensive so we only
 * refetch the data if its stamp changed.
 */
class DeviceCache {
    // Stores an interface name and a {@link Plugin.ScanHandler}.
    private static class Scanner {
        private final String interfaceName;
        private final Plugin.ScanHandler handler;

        private Scanner(String interfaceName, Plugin.ScanHandler handler) {
            this.interfaceName = interfaceName;
            this.handler = handler;
        }
    }

    // Stores advertisements from a device with a stamp.
    private static class CacheEntry {
        private final long stamp;
        private String deviceId;
        private Set<Equivalence.Wrapper<AdInfo>> adInfos;
        private Instant lastSeen;

        private CacheEntry(long stamp, String deviceId, Set<Equivalence.Wrapper<AdInfo>> adInfos) {
            this.stamp = stamp;
            this.deviceId = deviceId;
            this.adInfos = adInfos;
            this.lastSeen = new Instant();
        }
    }

    private static final Equivalence<AdInfo> ADINFO_EQUIVALENCE =
            new Equivalence<AdInfo>() {
                @Override
                protected boolean doEquivalent(AdInfo a, AdInfo b) {
                    return a.getAd().getId().equals(b.getAd().getId())
                            && a.getHash().equals(b.getHash());
                }

                @Override
                protected int doHash(AdInfo adinfo) {
                    return Objects.hashCode(adinfo.getAd().getId(), adinfo.getHash());
                }
            };
    private static final Function<AdInfo, Equivalence.Wrapper<AdInfo>> ADINFO_WRAPPER =
            new Function<AdInfo, Equivalence.Wrapper<AdInfo>>() {
                @Override
                public Equivalence.Wrapper<AdInfo> apply(AdInfo adinfo) {
                    return ADINFO_EQUIVALENCE.wrap(adinfo);
                }
            };

    private final Map<Long, CacheEntry> cacheByStamp = new HashMap<>();
    private final Map<String, CacheEntry> cacheByDeviceId = new HashMap<>();

    private final SetMultimap<String, Equivalence.Wrapper<AdInfo>> adInfosByInterfaceName =
            HashMultimap.create();

    private final SetMultimap<String, Scanner> scannersByInterfaceName = HashMultimap.create();
    private final Map<Plugin.ScanHandler, Scanner> scannersByHandler = new HashMap<>();

    private final Duration maxAge;
    private ScheduledExecutorService timer;

    DeviceCache(Duration maxAge) {
        this.maxAge = maxAge;
        this.timer = Executors.newSingleThreadScheduledExecutor();
        long periodicity = maxAge.getMillis() / 2;
        timer.scheduleAtFixedRate(
                new Runnable() {
                    @Override
                    public void run() {
                        removeStaleEntries();
                    }
                },
                periodicity,
                periodicity,
                TimeUnit.MILLISECONDS);
    }

    private void removeStaleEntries() {
        synchronized (this) {
            Iterator<Map.Entry<Long, CacheEntry>> it = cacheByStamp.entrySet().iterator();
            while (it.hasNext()) {
                CacheEntry entry = it.next().getValue();
                if (entry.lastSeen.plus(maxAge).isBeforeNow()) {
                    it.remove();
                    cacheByDeviceId.remove(entry.deviceId);
                    for (Equivalence.Wrapper<AdInfo> wrapper : entry.adInfos) {
                        AdInfo adinfo = wrapper.get();
                        adInfosByInterfaceName.remove(adinfo.getAd().getInterfaceName(), wrapper);
                        adinfo.setLost(true);
                        handleUpdate(adinfo);
                    }
                }
            }
        }
    }

    /**
     * Cleans up the cache's state and shutdowns the eviction thread.
     */
    void shutdownCache() {
        timer.shutdown();
    }

    /**
     * Returns whether this stamp has been seen before.
     *
     * @param stamp     the stamp of the advertisement
     * @param deviceId  the deviceId of the advertisement (used to handle rotating ids)
     * @return          true iff this stamp is in the cache
     */
    boolean haveSeenStamp(long stamp, String deviceId) {
        synchronized (this) {
            CacheEntry entry = cacheByStamp.get(stamp);
            if (entry != null) {
                entry.lastSeen = new Instant();
                if (!entry.deviceId.equals(deviceId)) {
                    // This probably happened because a device has changed it's ble mac address.
                    // We need to update the mac address for this entry.
                    cacheByDeviceId.remove(entry.deviceId);
                    entry.deviceId = deviceId;
                    cacheByDeviceId.put(deviceId, entry);
                }
            }
            return entry != null;
        }
    }

    /**
     * Saves the set of advertisements and stamp for this device.
     *
     * @param stamp     the stamp provided by the device
     * @param deviceId  the id of the device
     * @param adinfos   the advertisements exposed by the device
     */
    void saveDevice(long stamp, String deviceId, Iterable<AdInfo> adInfos) {
        Set<Equivalence.Wrapper<AdInfo>> newAdInfos =
                FluentIterable.from(adInfos).transform(ADINFO_WRAPPER).toSet();
        CacheEntry entry = new CacheEntry(stamp, deviceId, newAdInfos);
        synchronized (this) {
            Set<Equivalence.Wrapper<AdInfo>> oldAdInfos;
            CacheEntry oldEntry = cacheByDeviceId.remove(deviceId);
            if (oldEntry != null) {
                cacheByStamp.remove(oldEntry.stamp);
                oldAdInfos = oldEntry.adInfos;
            } else {
                oldAdInfos = ImmutableSet.of();
            }

            Set<Equivalence.Wrapper<AdInfo>> removed = Sets.difference(oldAdInfos, newAdInfos);
            for (Equivalence.Wrapper<AdInfo> wrapped : removed) {
                AdInfo adInfo = wrapped.get();
                adInfosByInterfaceName.remove(adInfo.getAd().getInterfaceName(), wrapped);
                adInfo.setLost(true);
                handleUpdate(adInfo);
            }
            Set<Equivalence.Wrapper<AdInfo>> added = Sets.difference(newAdInfos, oldAdInfos);
            for (Equivalence.Wrapper<AdInfo> wrapped : added) {
                AdInfo adInfo = wrapped.get();
                adInfosByInterfaceName.put(adInfo.getAd().getInterfaceName(), wrapped);
                handleUpdate(adInfo);
            }
            cacheByStamp.put(stamp, entry);
            cacheByDeviceId.put(deviceId, entry);
        }
    }

    private void handleUpdate(AdInfo adinfo) {
        Set<Scanner> scanners = scannersByInterfaceName.get("");
        if (scanners != null) {
            for (Scanner scanner : scanners) {
                scanner.handler.handleUpdate(adinfo);
            }
        }
        scanners = scannersByInterfaceName.get(adinfo.getAd().getInterfaceName());
        if (scanners != null) {
            for (Scanner scanner : scanners) {
                scanner.handler.handleUpdate(adinfo);
            }
        }
    }

    /**
     * Adds a scan handler for advertisements that match {@link interfaceName}.
     * <p>
     * If {@link handler} already exists, the old handler is replaced.
     */
    void addScanner(String interfaceName, Plugin.ScanHandler handler) {
        Scanner scanner = new Scanner(interfaceName, handler);
        synchronized (this) {
            scannersByHandler.put(handler, scanner);
            scannersByInterfaceName.put(interfaceName, scanner);

            Iterable<Equivalence.Wrapper<AdInfo>> adinfos;
            if (interfaceName.isEmpty()) {
                adinfos = adInfosByInterfaceName.values();
            } else {
                adinfos = adInfosByInterfaceName.get(interfaceName);
            }
            if (adinfos != null) {
                for (Equivalence.Wrapper<AdInfo> wrapper : adinfos) {
                    scanner.handler.handleUpdate(wrapper.get());
                }
            }
        }
    }

    /**
     * Removes the scan handler.
     */
    void removeScanner(Plugin.ScanHandler handler) {
        synchronized (this) {
            Scanner scanner = scannersByHandler.remove(handler);
            if (scanner != null) {
                scannersByInterfaceName.remove(scanner.interfaceName, scanner);
            }
        }
    }
}
