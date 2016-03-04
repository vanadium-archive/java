// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit.bind;

import android.widget.ListView;

import com.google.common.base.Throwables;

import io.v.baku.toolkit.VAndroidTestCase;
import io.v.rx.syncbase.RxAndroidSyncbase;
import io.v.rx.syncbase.RxTable;

public class CollectionBindingTest extends VAndroidTestCase {
    private RxAndroidSyncbase mRxSyncbase;
    private RxTable mTable;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mRxSyncbase = new RxAndroidSyncbase(getVContext(), createSyncbaseClient());
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
        try (final RxListAdapter<RxTable.Row<String>> adapter = new BindingBuilder()
                .onError(t -> fail(Throwables.getStackTraceAsString(t)))
                .viewAdapterContext(getContext())
                .rxTable(mTable)
                .onPrefix("Good")
                .type(String.class)
                .bindTo(listView)) {

            pause();

            assertEquals(2, listView.getCount());
            assertEquals("Goodnight", adapter.getItem(0).getRowName());
            assertEquals("moon", adapter.getItem(0).getValue());
            assertEquals("Good morning", adapter.getItem(1).getRowName());
            assertEquals("starshine", adapter.getItem(1).getValue());

            start(mTable.put("Goodbye", "Mr. Bond"));
            pause();

            assertEquals("Goodbye", adapter.getItem(0).getRowName());
            assertEquals("Mr. Bond", adapter.getItem(0).getValue());

            start(mTable.delete("Good morning"));
            pause();

            assertEquals(1, adapter.getRowIndex("Goodnight"));
            assertEquals("moon", adapter.getItem(1).getValue());
        }
    }
}
