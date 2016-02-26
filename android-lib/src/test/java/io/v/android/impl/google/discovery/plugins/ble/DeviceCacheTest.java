// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.impl.google.discovery.plugins.ble;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.ArrayList;
import java.util.Random;

import junit.framework.TestCase;

import org.joda.time.Duration;

import org.junit.Test;

import io.v.v23.discovery.AdId;
import io.v.v23.discovery.Advertisement;
import io.v.v23.discovery.Attributes;
import io.v.v23.discovery.Attachments;

import io.v.x.ref.lib.discovery.AdInfo;
import io.v.x.ref.lib.discovery.AdHash;
import io.v.x.ref.lib.discovery.EncryptionAlgorithm;
import io.v.x.ref.lib.discovery.EncryptionKey;

import static com.google.common.truth.Truth.assertThat;
import static io.v.impl.google.lib.discovery.DiscoveryTestUtil.assertThat;

/**
 * Tests for {@link DeviceCache}.
 */
public class DeviceCacheTest extends TestCase {
    private static Random rand = new Random();

    private static byte[] randBytes(int size) {
        byte[] bytes = new byte[size];
        rand.nextBytes(bytes);
        return bytes;
    }

    private static AdInfo newAdInfo(String interfaceName) {
        return new AdInfo(
                new Advertisement(
                        new AdId(randBytes(AdId.VDL_TYPE.getLength())),
                        interfaceName,
                        ImmutableList.<String>of(),
                        new Attributes(),
                        new Attachments()),
                new EncryptionAlgorithm(),
                ImmutableList.<EncryptionKey>of(),
                new AdHash(randBytes(AdHash.VDL_TYPE.getLength())),
                ImmutableList.<String>of(),
                false);
    }

    private static AdInfo copyAdInfo(AdInfo adinfo) {
        return new AdInfo(
                new Advertisement(
                        adinfo.getAd().getId(),
                        adinfo.getAd().getInterfaceName(),
                        adinfo.getAd().getAddresses(),
                        adinfo.getAd().getAttributes(),
                        adinfo.getAd().getAttachments()),
                adinfo.getEncryptionAlgorithm(),
                adinfo.getEncryptionKeys(),
                adinfo.getHash(),
                adinfo.getDirAddrs(),
                adinfo.getLost());
    }

    private static List<AdInfo> newAdInfoList(AdInfo... adinfos) {
        return Lists.transform(
                ImmutableList.copyOf(adinfos),
                new Function<AdInfo, AdInfo>() {
                    @Override
                    public AdInfo apply(AdInfo adinfo) {
                        return copyAdInfo(adinfo);
                    }
                });
    }

    private static class MockHandler implements Plugin.ScanHandler {
        private List<AdInfo> updates = new ArrayList<>();

        @Override
        public synchronized void handleUpdate(AdInfo adinfo) {
            updates.add(copyAdInfo(adinfo));
            notifyAll();
        }
    }

    public void testSaveDeveice() {
        DeviceCache cache = new DeviceCache(Duration.standardMinutes(10));
        // The advertisements here are not relevant since we are just checking
        // the seen stamp function.
        long stamp = 10001;
        assertThat(cache.haveSeenStamp(stamp, "device")).isFalse();
        cache.saveDevice(stamp, "device", ImmutableList.<AdInfo>of());
        assertThat(cache.haveSeenStamp(stamp, "device")).isTrue();
        cache.shutdownCache();
    }

    public void testSaveDeviceWithDifferentStampCode() {
        DeviceCache cache = new DeviceCache(Duration.standardMinutes(10));
        // The advertisements here are not relevant since we are just checking
        // the seen stamp function.
        long stamp = 10001;
        assertThat(cache.haveSeenStamp(stamp, "device")).isFalse();
        cache.saveDevice(stamp, "device", ImmutableList.<AdInfo>of());
        assertThat(cache.haveSeenStamp(stamp, "device")).isTrue();
        cache.saveDevice(stamp + 1, "device", ImmutableList.<AdInfo>of());
        assertThat(cache.haveSeenStamp(stamp + 1, "device")).isTrue();
        assertThat(cache.haveSeenStamp(stamp, "device")).isFalse();
        cache.shutdownCache();
    }

    public void testAddingScannerBeforeSavingDevice() {
        DeviceCache cache = new DeviceCache(Duration.standardMinutes(10));
        long stamp = 10001;

        AdInfo adinfo1 = newAdInfo("interface1");
        AdInfo adinfo2 = newAdInfo("interface2");

        MockHandler handler1 = new MockHandler();
        cache.addScanner("interface1", handler1);

        MockHandler handler2 = new MockHandler();
        cache.addScanner("", handler2);

        cache.saveDevice(stamp, "device", newAdInfoList(adinfo1, adinfo2));

        // Make sure that the handlers are called;
        assertThat(handler1.updates.size()).isEqualTo(1);
        assertThat(handler1.updates.get(0)).isEqualTo(adinfo1);
        assertThat(handler2.updates).isEqualTo(adinfo1, adinfo2);
        cache.shutdownCache();
    }

    public void testAddingScannerAfterSavingDevice() {
        DeviceCache cache = new DeviceCache(Duration.standardMinutes(10));
        long stamp = 10001;

        AdInfo adinfo1 = newAdInfo("interface1");
        AdInfo adinfo2 = newAdInfo("interface2");

        cache.saveDevice(stamp, "device", newAdInfoList(adinfo1, adinfo2));

        MockHandler handler1 = new MockHandler();
        cache.addScanner("interface1", handler1);

        MockHandler handler2 = new MockHandler();
        cache.addScanner("", handler2);

        // Make sure that the handlers are called;
        assertThat(handler1.updates.size()).isEqualTo(1);
        assertThat(handler1.updates.get(0)).isEqualTo(adinfo1);
        assertThat(handler2.updates).isEqualTo(adinfo1, adinfo2);
        cache.shutdownCache();
    }

    public void testRemovingAdvertisement() {
        DeviceCache cache = new DeviceCache(Duration.standardMinutes(10));
        long stamp = 10001;

        AdInfo adinfo1 = newAdInfo("interface1");
        AdInfo adinfo2 = newAdInfo("interface2");

        MockHandler handler = new MockHandler();
        cache.addScanner("interface1", handler);

        cache.saveDevice(stamp, "device", newAdInfoList(adinfo1, adinfo2));
        cache.saveDevice(stamp + 1, "device", newAdInfoList(adinfo2));

        // Make sure that the handler is called;
        assertThat(handler.updates.size()).isEqualTo(2);
        assertThat(handler.updates.get(0)).isEqualTo(adinfo1, false);
        assertThat(handler.updates.get(1)).isEqualTo(adinfo1, true);
        cache.shutdownCache();
    }

    public void testAddingSameAdvertisement() {
        DeviceCache cache = new DeviceCache(Duration.standardMinutes(10));
        long stamp = 10001;

        AdInfo adinfo = newAdInfo("interface1");

        MockHandler handler = new MockHandler();
        cache.addScanner("interface1", handler);

        cache.saveDevice(stamp, "device", newAdInfoList(adinfo));
        cache.saveDevice(stamp + 1, "device", newAdInfoList(adinfo));

        // Make sure that the handler is called;
        assertThat(handler.updates.size()).isEqualTo(1);
        assertThat(handler.updates.get(0)).isEqualTo(adinfo);
        cache.shutdownCache();
    }

    public void testRemovingScanner() {
        DeviceCache cache = new DeviceCache(Duration.standardMinutes(10));
        long stamp = 10001;

        AdInfo adinfo1 = newAdInfo("interface1");
        AdInfo adinfo2 = newAdInfo("interface2");

        MockHandler handler = new MockHandler();
        cache.addScanner("interface1", handler);

        cache.saveDevice(stamp, "device", newAdInfoList(adinfo1));

        assertThat(handler.updates.size()).isEqualTo(1);
        assertThat(handler.updates.get(0)).isEqualTo(adinfo1);

        cache.removeScanner(handler);

        cache.saveDevice(stamp, "device", newAdInfoList(adinfo2));

        // Make sure that the handler is not called any more;
        assertThat(handler.updates.size()).isEqualTo(1);
        cache.shutdownCache();
    }

    @Test(timeout = 30000)
    public void testCacheEviction() {
        // TODO(jhahn): Use a fake ScheduledExecutorService.
        DeviceCache cache = new DeviceCache(Duration.millis(2));
        long stamp = 10001;

        AdInfo adinfo = newAdInfo("interface1");

        MockHandler handler = new MockHandler();
        cache.addScanner("interface1", handler);

        cache.saveDevice(stamp, "device", newAdInfoList(adinfo));

        synchronized (handler) {
            try {
                while (handler.updates.size() < 2) {
                    handler.wait();
                }
            } catch (InterruptedException e) {
                // Keep waiting.
            }
        }

        // Make sure that the handler is called;
        assertThat(handler.updates.size()).isEqualTo(2);
        assertThat(handler.updates.get(0)).isEqualTo(adinfo, false);
        assertThat(handler.updates.get(1)).isEqualTo(adinfo, true);

        // Make sure that there is no cached entries.
        handler = new MockHandler();
        cache.addScanner("interface1", handler);
        assertThat(handler.updates.size()).isEqualTo(0);

        cache.shutdownCache();
    }
}
