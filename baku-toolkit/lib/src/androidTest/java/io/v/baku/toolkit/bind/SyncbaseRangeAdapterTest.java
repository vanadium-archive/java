// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit.bind;

import android.widget.ListView;

import com.google.common.base.Throwables;

import io.v.baku.toolkit.VAndroidTestCase;
import io.v.rx.syncbase.RxSyncbase;
import io.v.rx.syncbase.RxTable;

public class SyncbaseRangeAdapterTest extends VAndroidTestCase {
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

    public void test() throws Exception {
        await(parallel(
                mTable.put("Hello", "world"),
                mTable.put("Goodnight", "moon"),
                mTable.put("Good morning", "starshine")));

        final ListView listView = new ListView(getContext());
        try (final SyncbaseRangeAdapter<String> adapter = SyncbaseRangeAdapter.builder()
                .onError(t -> fail(Throwables.getStackTraceAsString(t)))
                .viewAdapterContext(getContext())
                .rxTable(mTable)
                .prefix("Good")
                .type(String.class)
                .bindTo(listView)
                .getAdapter()) {

            pause();

            assertEquals(2, listView.getCount());
            assertEquals("Goodnight", adapter.getRowName(0));
            assertEquals("moon", adapter.getItem(0));
            assertEquals("Good morning", adapter.getRowName(1));
            assertEquals("starshine", adapter.getItem(1));

            start(mTable.put("Goodbye", "Mr. Bond"));
            pause();

            assertEquals("Goodbye", adapter.getRowName(0));
            assertEquals("Mr. Bond", adapter.getItem(0));

            start(mTable.delete("Good morning"));
            pause();

            assertEquals(1, adapter.getRowIndex("Goodnight"));
            assertEquals("moon", adapter.getItem(1));
        }
    }
}
