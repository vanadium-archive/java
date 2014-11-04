package io.veyron.veyron.veyron2.vdl;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * VdlMap is a representation of a VDL map.
 * It is a wrapper around {@code java.util.Map} that stores a VDL {@code Type}.
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 */
public class VdlMap<K, V> extends VdlValue implements Map<K, V>, Parcelable {
    private final Map<K, V> impl;

    /**
     * Wraps a map with a VDL value.
     *
     * @param type runtime VDL type of the wrapped map
     * @param impl wrapped map
     */
    public VdlMap(VdlType type, Map<K, V> impl) {
        super(type);
        assertKind(Kind.MAP);
        this.impl = impl;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (this.getClass() != obj.getClass()) return false;
        final VdlMap<?, ?> other = (VdlMap<?, ?>)obj;
        return this.impl.equals(other.impl);
    }

    @Override
    public int hashCode() {
        return (impl == null) ? 0 : impl.hashCode();
    }

    @Override
    public void clear() {
        impl.clear();
    }

    @Override
    public boolean containsKey(Object key) {
        return impl.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return impl.containsValue(value);
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        return impl.entrySet();
    }

    @Override
    public V get(Object key) {
        return impl.get(key);
    }

    @Override
    public boolean isEmpty() {
        return impl.isEmpty();
    }

    @Override
    public Set<K> keySet() {
        return impl.keySet();
    }

    @Override
    public V put(K key, V value) {
        return impl.put(key, value);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        impl.putAll(map);
    }

    @Override
    public V remove(Object key) {
        return impl.remove(key);
    }

    @Override
    public int size() {
        return impl.size();
    }

    @Override
    public Collection<V> values() {
        return impl.values();
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
    public static final Creator<VdlMap> CREATOR = new Creator<VdlMap>() {
        @Override
        public VdlMap createFromParcel(Parcel in) {
            return (VdlMap) in.readSerializable();
        }

        @Override
        public VdlMap[] newArray(int size) {
            return new VdlMap[size];
        }
    };
}
