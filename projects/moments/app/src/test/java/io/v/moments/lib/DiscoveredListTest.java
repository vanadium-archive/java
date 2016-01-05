// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.moments.lib;

import android.os.Handler;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import io.v.moments.ifc.AdConverter;
import io.v.moments.ifc.HasId;
import io.v.moments.ifc.IdSet;
import io.v.moments.ifc.ListObserver;
import io.v.v23.discovery.Found;
import io.v.v23.discovery.Lost;
import io.v.v23.discovery.Update;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DiscoveredListTest {
    static final Thing THING0 = makeThing("hey0");
    static final Thing THING1 = makeThing("hey1");
    static final Id ID0 = THING0.getId();
    static final Id ID1 = THING1.getId();

    @Rule
    public ExpectedException mThrown = ExpectedException.none();

    @Mock
    ListObserver mObserver;
    @Mock
    AdConverter<Thing> mConverter;
    @Mock
    IdSet mRejects;
    @Mock
    Handler mHandler;
    @Mock
    io.v.v23.discovery.Service mAdvertisement;

    @Captor
    ArgumentCaptor<Runnable> mRunnable;


    DiscoveredList<Thing> mList;

    static Thing makeThing(String v) {
        return new Thing(v, Id.makeRandom());
    }

    @Before
    public void setup() {
        mList = new DiscoveredList<>(mConverter, mRejects, mHandler);
    }

    @Test
    public void newListEmpty() throws Exception {
        assertEquals(0, mList.size());
    }

    @Test
    public void constructorFailsWithNoConverter() throws Exception {
        mThrown.expect(IllegalStateException.class);
        mThrown.expectMessage("Null converter");
        mList = new DiscoveredList<>(null, mRejects, mHandler);
    }

    @Test
    public void constructorFailsWithNoIdSet() throws Exception {
        mThrown.expect(IllegalStateException.class);
        mThrown.expectMessage("Null rejects");
        mList = new DiscoveredList<>(mConverter, null, mHandler);
    }

    @Test
    public void constructorFailsWithNoHandler() throws Exception {
        mThrown.expect(IllegalStateException.class);
        mThrown.expectMessage("Null handler");
        mList = new DiscoveredList<>(mConverter, mRejects, null);
    }

    @Test
    public void addFailsWithNoObserver() throws Exception {
        mThrown.expect(IllegalStateException.class);
        mThrown.expectMessage("No observer for add");
        mList.push(THING0);
    }

    @Test
    public void setFailsWithNoObserver() throws Exception {
        mThrown.expect(IllegalStateException.class);
        mThrown.expectMessage("No observer for set");
        mList.set(0, THING1);
    }

    @Test
    public void changeFailsWithNoObserver() throws Exception {
        mThrown.expect(IllegalStateException.class);
        mThrown.expectMessage("No observer for change");
        mList.change(0);
    }

    @Test
    public void removeFailsWithNoObserver() throws Exception {
        mThrown.expect(IllegalStateException.class);
        mThrown.expectMessage("No observer for remove");
        mList.remove(0);
    }

    @Test
    public void insertionWorks() throws Exception {
        mList.setObserver(mObserver);
        mList.push(THING0);
        verify(mObserver).notifyItemInserted(0);
        assertEquals(1, mList.size());
        assertEquals(THING0, mList.get(0));
    }

    @Test
    public void handleNormalFound() throws Exception {
        mList.setObserver(mObserver);

        // Make sure the advertisement isn't rejected as a self-ad.
        when(mAdvertisement.getInstanceId()).thenReturn(ID0.toString());
        when(mRejects.contains(ID0)).thenReturn(false);
        when(mConverter.make(mAdvertisement)).thenReturn(THING0);

        mList.handleUpdate(new Update.Found(new Found(mAdvertisement)));

        verify(mHandler).post(mRunnable.capture());
        mRunnable.getValue().run();
        assertEquals(1, mList.size());
        assertEquals(THING0, mList.get(0));
        verify(mObserver).notifyItemInserted(0);
    }

    @Test
    public void handleSelfFound() throws Exception {
        mList.setObserver(mObserver);

        when(mAdvertisement.getInstanceId()).thenReturn(ID0.toString());
        when(mRejects.contains(ID0)).thenReturn(true);

        mList.handleUpdate(new Update.Found(new Found(mAdvertisement)));

        verifyZeroInteractions(mHandler);
        verifyZeroInteractions(mObserver);
        assertEquals(0, mList.size());
    }

    @Test
    public void handleUnrecognizedLost() throws Exception {
        mList.setObserver(mObserver);

        when(mRejects.contains(ID0)).thenReturn(false);
        mList.handleUpdate(new Update.Lost(new Lost(ID0.toString())));

        verify(mHandler).post(mRunnable.capture());
        mRunnable.getValue().run();

        // Currently nothing happens - not even an error.
        assertEquals(0, mList.size());
        verifyZeroInteractions(mHandler);
        verifyZeroInteractions(mObserver);
    }

    @Test
    public void handleSelfLost() throws Exception {
        mList.setObserver(mObserver);

        when(mRejects.contains(ID0)).thenReturn(true);
        mList.handleUpdate(new Update.Lost(new Lost(ID0.toString())));

        verify(mHandler).post(mRunnable.capture());
        mRunnable.getValue().run();

        // Currently nothing happens.
        assertEquals(0, mList.size());
        verifyZeroInteractions(mHandler);
        verifyZeroInteractions(mObserver);
    }

    @Test
    public void handleNormalLost() throws Exception {
        mList.setObserver(mObserver);
        mList.add(0, THING0);
        verify(mObserver).notifyItemInserted(0);
        assertEquals(1, mList.size());

        when(mRejects.contains(ID0)).thenReturn(false);
        mList.handleUpdate(new Update.Lost(new Lost(ID0.toString())));

        verify(mHandler).post(mRunnable.capture());

        mRunnable.getValue().run();

        assertEquals(0, mList.size());
        verify(mObserver).notifyItemRemoved(0);
    }

    @Test
    public void dropAll() throws Exception {
        mList.setObserver(mObserver);
        mList.push(THING0);
        mList.push(THING1);
        verify(mObserver, times(2)).notifyItemInserted(0);
        assertEquals(2, mList.size());

        when(mRejects.contains(ID0)).thenReturn(false);
        when(mRejects.contains(ID1)).thenReturn(false);
        mList.dropAll();

        verify(mHandler).post(mRunnable.capture());

        mRunnable.getValue().run();

        assertEquals(0, mList.size());
        verify(mObserver).notifyItemRemoved(1);
        verify(mObserver).notifyItemRemoved(0);
        verifyNoMoreInteractions(mObserver);
    }

    @Test
    public void badInsertionFails() throws Exception {
        mThrown.expect(IndexOutOfBoundsException.class);
        mList.setObserver(mObserver);
        mList.add(333, THING0);
        verify(mObserver, never()).notifyItemInserted(333);
    }

    @Test
    public void removeWorks() throws Exception {
        mList.setObserver(mObserver);
        mList.add(0, THING0);
        verify(mObserver).notifyItemInserted(0);
        mList.add(1, THING1);
        verify(mObserver).notifyItemInserted(1);
        assertEquals(2, mList.size());
        assertEquals(THING0, mList.get(0));
        assertEquals(THING1, mList.get(1));
        assertEquals(THING0, mList.remove(0));
        verify(mObserver).notifyItemRemoved(0);
        assertEquals(1, mList.size());
        assertEquals(THING1, mList.get(0));
    }

    @Test
    public void badRemoveFails() throws Exception {
        mThrown.expect(IndexOutOfBoundsException.class);
        mList.setObserver(mObserver);
        mList.remove(333);
        verifyZeroInteractions(mObserver);
    }

    @Test
    public void setWorks() throws Exception {
        mList.setObserver(mObserver);
        mList.add(0, THING0);
        verify(mObserver).notifyItemInserted(0);
        assertEquals(1, mList.size());
        assertEquals(THING0, mList.get(0));
        mList.set(0, THING1);
        assertEquals(1, mList.size());
        assertEquals(THING1, mList.get(0));
        verify(mObserver).notifyItemChanged(0);
    }

    @Test
    public void badSetFails() throws Exception {
        mThrown.expect(IndexOutOfBoundsException.class);
        mList.setObserver(mObserver);
        mList.set(0, THING0);
        verifyZeroInteractions(mObserver);
    }

    @Test
    public void changeWorks() throws Exception {
        mList.setObserver(mObserver);
        mList.add(0, THING0);
        verify(mObserver).notifyItemInserted(0);
        // Not truly changing, since there's no check for it.
        mList.change(0);
        verify(mObserver).notifyItemChanged(0);
    }

    @Test
    public void badChangeFails() throws Exception {
        mThrown.expect(IndexOutOfBoundsException.class);
        mList.setObserver(mObserver);
        mList.change(333);
        verifyZeroInteractions(mObserver);
    }

    static class Thing implements HasId {
        private String mValue;
        private Id mId;

        public Thing(String v, Id id) {
            mValue = v;
            mId = id;
        }

        public Id getId() {
            return mId;
        }
    }
}
