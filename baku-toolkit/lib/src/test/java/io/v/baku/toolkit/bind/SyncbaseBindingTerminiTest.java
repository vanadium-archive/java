// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit.bind;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.SettableFuture;

import org.junit.Test;

import io.v.rx.RxTestCase;
import io.v.rx.syncbase.RxTable;
import io.v.v23.syncbase.nosql.Table;
import rx.subjects.PublishSubject;
import rx.subjects.ReplaySubject;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class SyncbaseBindingTerminiTest extends RxTestCase {
    @Test
    public void testSequencing() {
        final ReplaySubject<Table> mockTables = ReplaySubject.createWithSize(1);
        final RxTable rxTable = mock(RxTable.class);
        when(rxTable.getObservable()).thenReturn(mockTables);
        when(rxTable.once()).thenCallRealMethod();

        final PublishSubject<Integer> rxData = PublishSubject.create();

        final String key = "key";
        SyncbaseBindingTermini.bindWrite(rxTable, rxData, key, Integer.class, null,
                this::catchAsync);

        rxData.onNext(1);
        rxData.onNext(2);

        final Table t1 = mock(Table.class);
        when(t1.put(any(), any(), any(), any())).thenReturn(Futures.immediateFuture(null));

        mockTables.onNext(t1);
        verify(t1, never()).put(null, key, 1, Integer.class);
        verify(t1, timeout(BLOCKING_DELAY_MS) ).put(null, key, 2, Integer.class);

        rxData.onNext(3);
        verify(t1, timeout(BLOCKING_DELAY_MS)).put(null, key, 3, Integer.class);

        final Table t2 = mock(Table.class);
        final SettableFuture<Void> putComplete = SettableFuture.create();
        when(t2.put(any(), any(), any(), any())).thenReturn(putComplete);

        mockTables.onNext(t2);

        rxData.onNext(4);
        verify(t2, timeout(BLOCKING_DELAY_MS)).put(null, key, 4, Integer.class);

        rxData.onNext(5);

        rxData.onNext(6);
        putComplete.set(null);

        verify(t2, never()).put(null, key, 5, Integer.class);
        verify(t2, timeout(BLOCKING_DELAY_MS)).put(null, key, 6, Integer.class);

        verifyNoMoreInteractions(t1);
        verifyNoMoreInteractions(t2);
    }
}
