// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.moments.lib;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import io.v.moments.ifc.HasId;
import io.v.moments.ifc.ListObserver;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

@RunWith(MockitoJUnitRunner.class)
public class ObservedListTest {
    static final Thing THING0 = makeThing("hey0");
    static final Thing THING1 = makeThing("hey1");
    @Rule
    public ExpectedException mThrown = ExpectedException.none();
    @Mock
    ListObserver mObserver;
    ObservedList<Thing> mList = new ObservedList<>();

    static Thing makeThing(String v) {
        return new Thing(v, Id.makeRandom());
    }

    @Test
    public void newListEmpty() throws Exception {
        assertEquals(0, mList.size());
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
