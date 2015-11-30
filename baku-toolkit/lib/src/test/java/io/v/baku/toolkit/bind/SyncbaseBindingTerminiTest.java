// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit.bind;

import org.junit.Test;

import io.v.rx.RxTestCase;
import io.v.rx.syncbase.RxTable;
import io.v.v23.syncbase.nosql.Table;
import rx.subjects.PublishSubject;
import rx.subjects.ReplaySubject;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class SyncbaseBindingTerminiTest extends RxTestCase {
    @Test
    public void testSequencing() throws Exception {
        final ReplaySubject<Table> mockTables = ReplaySubject.createWithSize(1);
        final RxTable rxTable = mock(RxTable.class);
        when(rxTable.getObservable()).thenReturn(mockTables);
        when(rxTable.once()).thenCallRealMethod();

        final PublishSubject<Integer> rxData = PublishSubject.create();

        final String key = "key";
        SyncbaseBindingTermini.bindWrite(rxTable, rxData, key, Integer.class, null,
                this::catchAsync);

        rxData.onNext(1);
        Thread.sleep(BLOCKING_DELAY_MS);

        rxData.onNext(2);
        Thread.sleep(BLOCKING_DELAY_MS);

        final Table t1 = mock(Table.class);
        mockTables.onNext(t1);
        Thread.sleep(BLOCKING_DELAY_MS);
        verify(t1).put(null, key, 2, Integer.class);

        rxData.onNext(3);
        Thread.sleep(BLOCKING_DELAY_MS);
        verify(t1).put(null, key, 3, Integer.class);

        verifyNoMoreInteractions(t1);

        final Table t2 = mock(Table.class);
        mockTables.onNext(t2);
        Thread.sleep(BLOCKING_DELAY_MS);
        verifyZeroInteractions(t2);

        rxData.onNext(4);
        Thread.sleep(BLOCKING_DELAY_MS);
        verify(t2).put(null, key, 4, Integer.class);
    }
}
