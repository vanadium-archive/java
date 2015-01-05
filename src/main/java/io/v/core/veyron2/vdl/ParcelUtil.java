package io.v.core.veyron2.vdl;

import android.os.Parcel;
import android.os.Parcelable;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ParcelUtil contains utility methods for parceling various VDL types.
 */
public class ParcelUtil {
    /**
     * Writes a {@code java.util.List} value into a parcel.
     *
     * @param out the parcel where the value will be written.
     * @param list the list to be written into the parcel.
     * @param elementType a type of elements in the list.
     */
    public static void writeList(Parcel out, List<?> list, Type elementType) {
        out.writeInt(list.size());
        for (Object element : list) {
            writeValue(out, element, elementType);
        }
    }

    /**
     * Writes a {@code java.util.Set} value into a parcel.
     *
     * @param out the parcel where the value will be written.
     * @param set the value to be written into the parcel.
     * @param keyType a type of keys in the set.
     */
    public static void writeSet(Parcel out, Set<?> set, Type keyType) {
        out.writeInt(set.size());
        for (Object key : set) {
            writeValue(out, key, keyType);
        }
    }

    /**
     * Writes a {@code java.util.Map} value into a parcel.
     *
     * @param out the parcel where the value will be written.
     * @param map the map to be written into the parcel.
     * @param keyType a type of keys in the map.
     * @param valueType a type of values in the map.
     */
    public static void writeMap(Parcel out, Map<?, ?> map, Type keyType, Type valueType) {
        out.writeInt(map.size());
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            writeValue(out, entry.getKey(), keyType);
            writeValue(out, entry.getValue(), valueType);
        }
    }

    /**
     * Writes a value into a parcel.
     *
     * @param out the parcel where the value will be written.
     * @param value the object to be written into the parcel.
     * @param type a type the object.
     */
    @SuppressWarnings("unchecked")
    public static void writeValue(Parcel out, Object value, Type type) {
        if (type instanceof Class) {
            Class<?> klass = (Class<?>) type;
            if (Parcelable.class.isAssignableFrom(klass)) {
                out.writeParcelable((Parcelable) value, 0);
                return;
            }
        } else if (type instanceof ParameterizedType) {
            Class<?> klass = (Class<?>) ((ParameterizedType) type).getRawType();
            Type[] elementTypes = ((ParameterizedType) type).getActualTypeArguments();
            if (List.class.isAssignableFrom(klass)) {
                writeList(out, (List<?>) value, elementTypes[0]);
                return;
            } else if (Set.class.isAssignableFrom(klass)) {
                writeSet(out, (Set<?>) value, elementTypes[0]);
                return;
            } else if (Map.class.isAssignableFrom(klass)) {
                writeMap(out, (Map<?, ?>) value, elementTypes[0], elementTypes[1]);
                return;
            }
        }
        out.writeValue(value);
    }

    /**
     * Reads a {@code java.util.List} from a parcel.
     *
     * @param in the parcel from which the value should be read.
     * @param loader a ClassLoader to be used while reading from the parcel.
     * @param elementType a type of elements in the list.
     */
    public static ArrayList<Object> readList(Parcel in, ClassLoader loader, Type elementType) {
        int size = in.readInt();
        ArrayList<Object> list = new ArrayList<Object>();
        for (int i = 0; i < size; i++) {
            list.add(readValue(in, loader, elementType));
        }
        return list;
    }

    /**
     * Reads a {@code java.util.Set} from a parcel.
     *
     * @param in the parcel from which the value should be read.
     * @param loader a ClassLoader to be used while reading from the parcel.
     * @param keyType a type of keys in the set.
     */
    public static HashSet<Object> readSet(Parcel in, ClassLoader loader, Type keyType) {
        int size = in.readInt();
        HashSet<Object> set = new HashSet<Object>();
        for (int i = 0; i < size; i++) {
            set.add(readValue(in, loader, keyType));
        }
        return set;
    }

    /**
     * Reads a {@code java.util.Map} from a parcel.
     *
     * @param in the parcel from which the value should be read.
     * @param loader a ClassLoader to be used while reading from the parcel.
     * @param keyType a type of keys in the map.
     * @param valueType a type of values in the map.
     */
    public static HashMap<Object, Object> readMap(Parcel in, ClassLoader loader, Type keyType,
            Type valueType) {
        int size = in.readInt();
        HashMap<Object, Object> map = new HashMap<Object, Object>();
        for (int i = 0; i < size; i++) {
            map.put(readValue(in, loader, keyType), readValue(in, loader, valueType));
        }
        return map;
    }

    /**
     * Reads a value from a parcel.
     *
     * @param in the parcel from which the value should be read.
     * @param loader a ClassLoader to be used while reading from the parcel.
     * @param type a type of the value.
     */
    public static Object readValue(Parcel in, ClassLoader loader, Type type) {
        if (type instanceof Class) {
            Class<?> klass = (Class<?>) type;
            if (Parcelable.class.isAssignableFrom(klass)) {
                return in.readParcelable(loader);
            }
        } else if (type instanceof ParameterizedType) {
            Class<?> klass = (Class<?>) ((ParameterizedType) type).getRawType();
            Type[] elementTypes = ((ParameterizedType) type).getActualTypeArguments();
            if (List.class.isAssignableFrom(klass)) {
                return readList(in, loader, elementTypes[0]);
            } else if (Set.class.isAssignableFrom(klass)) {
                return readSet(in, loader, elementTypes[0]);
            } else if (Map.class.isAssignableFrom(klass)) {
                return readMap(in, loader, elementTypes[0], elementTypes[1]);
            }
        }
        return in.readValue(loader);
    }
}
