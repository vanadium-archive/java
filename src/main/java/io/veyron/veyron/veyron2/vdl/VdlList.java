package io.veyron.veyron.veyron2.vdl;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Collection;
import java.util.Iterator;
import java.util.ListIterator;

/**
 * VdlList is a representation of a VDL list.
 * It is a wrapper around {@code java.util.List} that stores a VDL {@code Type}.
 *
 * @param <T> The type of the list element.
 */
public class VdlList<T> extends VdlValue implements java.util.List<T>, Parcelable {
    private final java.util.List<T> impl;

    /**
     * Wraps a list with a VDL value.
     *
     * @param type runtime VDL type of the wrapped list
     * @param impl wrapped list
     */
    public VdlList(VdlType type, java.util.List<T> impl) {
        super(type);
        this.impl = impl;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (this.getClass() != obj.getClass()) return false;
        final VdlList<?> other = (VdlList<?>)obj;
        if (!(this.impl.equals(other.impl))) return false;
        return true;
    }

    @Override
    public int hashCode() {
        return (impl == null ? 0 : impl.hashCode());
    }

    @Override
    public void add(int location, T object) {
        impl.add(location, object);
    }

    @Override
    public boolean add(T object) {
        return impl.add(object);
    }

    @Override
    public boolean addAll(int location, Collection<? extends T> collection) {
        return impl.addAll(location, collection);
    }

    @Override
    public boolean addAll(Collection<? extends T> collection) {
        return impl.addAll(collection);
    }

    @Override
    public void clear() {
        impl.clear();
    }

    @Override
    public boolean contains(Object object) {
        return impl.contains(object);
    }

    @Override
    public boolean containsAll(Collection<?> collection) {
        return impl.containsAll(collection);
    }

    @Override
    public T get(int location) {
        return impl.get(location);
    }

    @Override
    public int indexOf(Object object) {
        return impl.indexOf(object);
    }

    @Override
    public boolean isEmpty() {
        return impl.isEmpty();
    }

    @Override
    public Iterator<T> iterator() {
        return impl.iterator();
    }

    @Override
    public int lastIndexOf(Object object) {
        return impl.lastIndexOf(object);
    }

    @Override
    public ListIterator<T> listIterator() {
        return impl.listIterator();
    }

    @Override
    public ListIterator<T> listIterator(int location) {
        return impl.listIterator(location);
    }

    @Override
    public T remove(int location) {
        return impl.remove(location);
    }

    @Override
    public boolean remove(Object object) {
        return impl.remove(object);
    }

    @Override
    public boolean removeAll(Collection<?> collection) {
        return impl.removeAll(collection);
    }

    @Override
    public boolean retainAll(Collection<?> collection) {
        return impl.retainAll(collection);
    }

    @Override
    public T set(int location, T object) {
        return impl.set(location, object);
    }

    @Override
    public int size() {
        return impl.size();
    }

    @Override
    public VdlList<T> subList(int start, int end) {
        return new VdlList<T>(getType(), impl.subList(start, end));
    }

    @Override
    public Object[] toArray() {
        return impl.toArray();
    }

    @Override
    public <T> T[] toArray(T[] array) {
        return impl.toArray(array);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeSerializable(this);
    }

    @SuppressWarnings("rawtypes")
    public static final Creator<VdlList> CREATOR = new Creator<VdlList>() {
        @Override
        public VdlList createFromParcel(Parcel in) {
            return (VdlList) in.readSerializable();
        }

        @Override
        public VdlList[] newArray(int size) {
            return new VdlList[size];
        }
    };
}
