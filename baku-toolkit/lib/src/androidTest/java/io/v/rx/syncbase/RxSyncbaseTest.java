// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.rx.syncbase;

import com.google.common.collect.ImmutableMap;

import junit.framework.Assert;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import io.v.baku.toolkit.VAndroidTestCase;
import io.v.v23.syncbase.nosql.RowRange;
import lombok.Getter;
import lombok.experimental.Accessors;
import rx.Observable;

@Accessors(prefix = "m")
@Getter
public class RxSyncbaseTest extends VAndroidTestCase {
    private RxSyncbase mRxSyncbase;
    private RxTable mTable;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mRxSyncbase = new RxSyncbase(getVContext(), createSyncbaseClient());
        mTable = mRxSyncbase.rxApp(getClass().getName()).rxDb("db").rxTable("t");
    }

    @Override
    protected void tearDown() throws Exception {
        await(mTable.destroy());
        mRxSyncbase.close();
        super.tearDown();
    }

    public void testPutGet() {
        await(parallel(
                sequence(
                        mTable.put("Hello", "world"),
                        mTable.get("Hello", String.class)
                                .doOnNext(s -> Assert.assertEquals("world", s))),
                sequence(
                        mTable.put("Goodnight", "moon"),
                        mTable.get("Goodnight", String.class)
                                .doOnNext(s -> Assert.assertEquals("moon", s)))));
    }

    public void testSingleWatch() {
        await(parallel(
                mTable.put("Hello", "world"),
                mTable.put("Goodnight", "moon")));
        final Iterator<SingleWatchEvent<String>> w =
                block(mTable.watch("Hello", String.class, "Goodbye"))
                        .getIterator();
        assertEquals("world", w.next().getValue());

        start(mTable.put("Hello", "Seattle"));
        assertEquals("Seattle", w.next().getValue());

        await(mTable.put("Goodnight", "my someone"));

        start(mTable.put("Hello", "Cleveland"));
        assertEquals("Cleveland", w.next().getValue());

        start(mTable.delete("Hello"));
        assertEquals("Goodbye", w.next().getValue());
    }

    private Iterator<? extends Map<String, String>> wrapWatch(
            final Observable<RangeWatchBatch<String>> watch) {
        return block(watch
                .concatMap(RangeWatchBatch::collectChanges)
                .scan(new HashMap<String, String>(), (data, events) -> {
                    for (final RangeWatchEvent<String> event : events) {
                        event.applyTo(data);
                    }
                    return data;
                }).skip(1) // skip the initial empty map
        ).getIterator();
    }

    public void testRangeWatch() throws Exception {
        await(parallel(
                mTable.put("Hello", "world"),
                mTable.put("Goodnight", "moon"),
                mTable.put("Good morning", "starshine")));

        final Observable<RangeWatchBatch<String>> watch =
                mTable.watch(RowRange.prefix("Good"), null, String.class);
        final Iterator<? extends Map<String, String>> w = wrapWatch(watch);
        assertEquals(ImmutableMap.of(
                "Goodnight", "moon",
                "Good morning", "starshine"), w.next());

        start(mTable.put("Goodnight", "my someone"));
        assertEquals("my someone", w.next().get("Goodnight"));

        await(mTable.put("Hello", "Seattle"));

        start(mTable.put("Good morning", "America"));
        assertEquals(ImmutableMap.of(
                "Goodnight", "my someone",
                "Good morning", "America"), w.next());

        start(mTable.delete("Goodnight"));
        assertEquals(ImmutableMap.of("Good morning", "America"), w.next());

        final Iterator<? extends Map<String, String>> w2 = wrapWatch(watch);
        assertEquals(ImmutableMap.of("Good morning", "America"), w2.next());
    }
}