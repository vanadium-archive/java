// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.rx.syncbase;

import com.google.common.util.concurrent.Futures;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import io.v.rx.RxTestCase;
import io.v.rx.SubscriberInputChannel;
import io.v.v23.context.VContext;
import io.v.v23.services.syncbase.nosql.KeyValue;
import io.v.v23.services.watch.ResumeMarker;
import io.v.v23.syncbase.nosql.BatchDatabase;
import io.v.v23.syncbase.nosql.ChangeType;
import io.v.v23.syncbase.nosql.Database;
import io.v.v23.syncbase.nosql.PrefixRange;
import io.v.v23.syncbase.nosql.RowRange;
import io.v.v23.syncbase.nosql.Table;
import io.v.v23.syncbase.nosql.WatchChange;
import io.v.v23.verror.EndOfFileException;
import io.v.v23.vom.VomUtil;
import rx.Observable;
import rx.subjects.PublishSubject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest({VomUtil.class, EndOfFileException.class})
public class RxTableTest extends RxTestCase {
    private static byte[] getRandomBytes() {
        final UUID uuid = UUID.randomUUID();
        final ByteArrayOutputStream bout = new ByteArrayOutputStream();
        final DataOutputStream dout = new DataOutputStream(bout);
        try {
            dout.writeLong(uuid.getMostSignificantBits());
            dout.writeLong(uuid.getLeastSignificantBits());
        } catch (final IOException e) {
            e.printStackTrace();
        }
        return bout.toByteArray();
    }

    private final PublishSubject<KeyValue> mInitial = PublishSubject.create();
    private final PublishSubject<WatchChange> mChanges = PublishSubject.create();
    private final Map<String, String> mData = new HashMap<>();

    @Before
    public void setUp() throws Exception {
        final PrefixRange prefix = RowRange.prefix("td");

        final SubscriberInputChannel<KeyValue> scanChan = new SubscriberInputChannel<>();
        final SubscriberInputChannel<WatchChange> watchChan = new SubscriberInputChannel<>();
        final ResumeMarker resumeMarker = new ResumeMarker();

        mInitial.subscribe(scanChan);
        mChanges.subscribe(watchChan);

        final VContext ctx = mock(VContext.class);
        final RxDb rxdb = mock(RxDb.class);
        final Database db = mock(Database.class);
        final BatchDatabase bdb = mock(BatchDatabase.class);
        final Table t = mock(Table.class);

        mockStatic(VomUtil.class);

        when(ctx.withCancel()).thenReturn(ctx);
        when(rxdb.getVContext()).thenReturn(ctx);
        when(rxdb.getObservable()).thenReturn(Observable.just(db));
        when(db.getTable("t")).thenReturn(t);
        when(bdb.getTable("t")).thenReturn(t);
        when(db.beginBatch(any(), any())).thenReturn(Futures.immediateFuture(bdb));
        when(bdb.getResumeMarker(any())).thenReturn(Futures.immediateFuture(resumeMarker));

        when(t.exists(any())).thenReturn(Futures.immediateFuture(true));
        when(t.scan(any(), eq(prefix))).thenReturn(scanChan);

        when(bdb.abort(any())).thenReturn(Futures.immediateFuture(null));
        when(db.watch(any(), eq("t"), eq(prefix.getPrefix()), eq(resumeMarker)))
                .thenReturn(watchChan);

        new RxTable("t", rxdb)
                .watch(prefix, null, String.class)
                .flatMap(RangeWatchBatch::getChanges)
                .subscribe(e -> e.applyTo(mData), this::catchAsync);
    }

    private void putInitial(final String rowName, final String value) throws Exception {
        final byte[] id = getRandomBytes();
        when(VomUtil.decode(id, String.class)).thenReturn(value);
        mInitial.onNext(new KeyValue(rowName, id));
    }

    private void putChange(final String rowName, final String value)
            throws Exception {
        final byte[] id = getRandomBytes();
        when(VomUtil.decode(id, String.class)).thenReturn(value);
        mChanges.onNext(new WatchChange("t", rowName, value == null ?
                ChangeType.DELETE_CHANGE : ChangeType.PUT_CHANGE,
                id, new ResumeMarker(), false, false));
    }

    private void putInitialError(final String rowName, final Throwable error) throws Exception {
        final byte[] id = getRandomBytes();
        when(VomUtil.decode(id, String.class)).thenThrow(error);
        mInitial.onNext(new KeyValue(rowName, id));
    }

    @Test
    public void testPrefixWatch() throws Exception {
        putInitial("Hello", "world");
        Thread.sleep(BLOCKING_DELAY_MS);
        assertEquals("world", mData.get("Hello"));

        putChange("Hello", "Seattle");
        putChange("Goodnight", "moon");
        Thread.sleep(BLOCKING_DELAY_MS);
        assertEquals("Seattle", mData.get("Hello"));
        assertEquals("moon", mData.get("Goodnight"));

        putChange("Hello", null);
        Thread.sleep(BLOCKING_DELAY_MS);
        assertFalse("Delete change", mData.containsKey("Hello"));
    }

    @Test
    public void testPrefixWatchInitialError() throws Exception {
        putInitialError("Hello", new RuntimeException("world"));
        Thread.sleep(BLOCKING_DELAY_MS);
        expect(RuntimeException.class);
    }
}
