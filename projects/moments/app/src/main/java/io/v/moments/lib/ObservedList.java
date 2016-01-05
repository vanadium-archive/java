// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.moments.lib;

import java.util.ArrayList;
import java.util.Iterator;

import io.v.moments.ifc.HasId;
import io.v.moments.ifc.ListObserver;

/**
 * List that notifies an observer of changes.
 */
public class ObservedList<T extends HasId> implements Iterable<T> {
    public static final int NOT_FOUND = -1;
    private final ArrayList<T> list = new ArrayList<>();
    private ListObserver mObserver;

    public void setObserver(ListObserver observer) {
        mObserver = observer;
    }

    public Iterator<T> iterator() {
        return list.iterator();
    }

    public boolean isEmpty() {
        return list.isEmpty();
    }

    public int size() {
        return list.size();
    }

    public T get(int index) {
        return list.get(index);
    }

    private IllegalStateException unobserved(String verb) {
        return new IllegalStateException("No observer for " + verb);
    }

    public void push(T item) {
        add(0, item);
    }

    public void add(int index, T item) {
        if (mObserver == null) {
            throw unobserved("add");
        }
        list.add(index, item);
        mObserver.notifyItemInserted(index);
    }

    public void set(int index, T item) {
        if (mObserver == null) {
            throw unobserved("set");
        }
        list.set(index, item);
        mObserver.notifyItemChanged(index);
    }

    /**
     * Signal a change at entry 'index' that doesn't involve a call to {@link
     * #set}.
     */
    public void change(int index) {
        if (mObserver == null) {
            throw unobserved("change");
        }
        if (index < 0 || index >= size()) {
            throw new IndexOutOfBoundsException();
        }
        mObserver.notifyItemChanged(index);
    }

    protected int findIndexWithId(Id id) {
        for (int i = 0; i < size(); i++) {
            if (get(i).getId().equals(id)) {
                return i;
            }
        }
        return NOT_FOUND;
    }

    public boolean hasId(Id id) {
        return findIndexWithId(id) != NOT_FOUND;
    }

    public void changeById(final Id id) {
        int index = findIndexWithId(id);
        if (index != NOT_FOUND) {
            change(index);
        }
    }

    public T remove(int index) {
        if (mObserver == null) {
            throw unobserved("remove");
        }
        T gone = list.remove(index);
        mObserver.notifyItemRemoved(index);
        return gone;
    }
}
