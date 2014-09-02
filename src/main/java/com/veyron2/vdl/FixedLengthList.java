
package com.veyron2.vdl;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

/**
 * FixedLengthList represents a list that always has a fixed length. It is
 * backed by an array.
 *
 * @param <T> The type of the list element.
 */
public final class FixedLengthList<T> implements List<T>, Serializable {
    static final long serialVersionUID = 0L;

    private final T[] backingArray;
    private final int start, end;

    /**
     * Creates a fixed length list for a specified class of the specified size.
     *
     * @param klass The list element type.
     * @param size The size of the list.
     */
    @SuppressWarnings("unchecked")
    public FixedLengthList(final Class<T> klass, final int size) {
        this.backingArray = (T[]) Array.newInstance(klass, size);
        this.start = 0;
        this.end = size;
    }

    /**
     * Creates a fixed length list based on a backing array. The backing array
     * isn't copied.
     *
     * @param backingArray The array that backs the fixed length list.
     */
    public FixedLengthList(final T[] backingArray) {
        this(backingArray, 0, backingArray.length);
    }

    /**
     * Creates a fixed length list based on a backing array. The backing array
     * isn't copied.
     *
     * @param backingArray The array that backs the fixed length list.
     * @param start The index in the array where the list should start
     *            (inclusive index)
     * @param end The index after the point in the array where the list should
     *            end (exclusive index).
     */
    public FixedLengthList(final T[] backingArray, final int start, final int end) {
        if (start < 0 || end > backingArray.length) {
            throw new IllegalArgumentException("indexes out of range of backing array");
        }
        this.backingArray = backingArray;
        this.start = start;
        this.end = end;
    }

    @Override
    public int hashCode() {
        // This specific hash code algorithm is required by the Java spec and
        // defined in the List interface Javadoc.
        int hashCode = 1;
        for (T e : this) {
            hashCode = 31 * hashCode + (e == null ? 0 : e.hashCode());
        }
        return hashCode;
    }

    private static boolean elementsEqual(Object e1, Object e2) {
        if (e1 == null) {
            return e2 == null;
        }
        return e1.equals(e2);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof List))
            return false;
        List<?> other = (List<?>) obj;
        if (other.size() != size()) {
            return false;
        }
        for (int i = 0; i < size(); i++) {
            Object e1 = other.get(i);
            Object e2 = get(i);
            if (!elementsEqual(e1, e2)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void add(int location, T object) {
        throw new UnsupportedOperationException("add() not supported");
    }

    @Override
    public boolean add(T object) {
        throw new UnsupportedOperationException("add() not supported");
    }

    @Override
    public boolean addAll(int location, Collection<? extends T> collection) {
        throw new UnsupportedOperationException("addAll() not supported");
    }

    @Override
    public boolean addAll(Collection<? extends T> collection) {
        throw new UnsupportedOperationException("addAll() not supported");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("clear() not supported");
    }

    @Override
    public boolean contains(Object obj) {
        return indexOf(obj) >= 0;
    }

    @Override
    public boolean containsAll(Collection<?> collection) {
        for (Object obj : collection) {
            if (!this.contains(obj)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public T get(int location) {
        if (location < start || location >= end) {
            throw new IndexOutOfBoundsException("index " + location + " outside of range [" + start
                    + "," + end + ")");
        }
        return backingArray[location];
    }

    @Override
    public int indexOf(Object object) {
        for (int i = start; i < end; i++) {
            T t = backingArray[i];
            if (elementsEqual(t, object)) {
                return i - start;
            }
        }
        return -1;
    }

    @Override
    public int lastIndexOf(Object object) {
        for (int i = end - 1; i >= start; i--) {
            T t = backingArray[i];
            if (elementsEqual(t, object)) {
                return i - start;
            }
        }
        return -1;
    }

    @Override
    public boolean isEmpty() {
        return start == end;
    }

    @Override
    public Iterator<T> iterator() {
        return listIterator();
    }

    @Override
    public ListIterator<T> listIterator() {
        return listIterator(0);
    }

    @Override
    public ListIterator<T> listIterator(int location) {
        return new FixedLengthListIterator(location);
    }

    @Override
    public T remove(int location) {
        throw new UnsupportedOperationException("remove() not supported");
    }

    @Override
    public boolean remove(Object object) {
        throw new UnsupportedOperationException("remove() not supported");
    }

    @Override
    public boolean removeAll(Collection<?> collection) {
        throw new UnsupportedOperationException("removeAll() not supported");
    }

    @Override
    public boolean retainAll(Collection<?> collection) {
        throw new UnsupportedOperationException("retainAll() not supported");
    }

    @Override
    public T set(int location, T object) {
        if (location < start || location >= end) {
            throw new IndexOutOfBoundsException("index " + location + " outside of range [" + start
                    + "," + end + ")");
        }
        T prev = backingArray[location];
        backingArray[location] = object;
        return prev;
    }

    @Override
    public int size() {
        return end - start;
    }

    @Override
    public List<T> subList(int start, int end) {
        return new FixedLengthList<T>(backingArray, start, end);
    }

    public T[] getBackingArray() {
        return backingArray;
    }

    @Override
    public Object[] toArray() {
        return Arrays.copyOfRange(backingArray, start, end);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <ToType> ToType[] toArray(ToType[] array) {
        if (array.length < size()) {
            return Arrays.copyOfRange(backingArray, start, end, (Class<ToType[]>) array.getClass());
        }
        System.arraycopy(backingArray, start, array, 0, size());
        return array;
    }

    private class FixedLengthListIterator implements ListIterator<T> {
        private int position;

        public FixedLengthListIterator(int index) {
            if (index < 0 || index >= FixedLengthList.this.size()) {
                throw new IllegalArgumentException("Index out of bounds");
            }
            position = index + start;
        }

        @Override
        public void add(T object) {
            throw new UnsupportedOperationException("add() not supported");
        }

        @Override
        public boolean hasNext() {
            return position < end - 1;
        }

        @Override
        public boolean hasPrevious() {
            return position > start;
        }

        @Override
        public T next() {
            if (position >= end) {
                throw new NoSuchElementException("beyond end of list");
            }
            return backingArray[position++];
        }

        @Override
        public int nextIndex() {
            if (position >= end) {
                return end - start;
            }
            return position - start;
        }

        @Override
        public T previous() {
            if (position < start) {
                throw new NoSuchElementException("before beginning of list");
            }
            return backingArray[position--];
        }

        @Override
        public int previousIndex() {
            if (position < start) {
                return 0;
            }
            return position - start;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("remove() not supported");
        }

        @Override
        public void set(T object) {
            throw new UnsupportedOperationException("set() not supported");
        }

    }

}
