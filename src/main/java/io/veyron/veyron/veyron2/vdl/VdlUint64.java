package io.veyron.veyron.veyron2.vdl;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.IOException;

/**
 * VdlUint64 is a representation of a VDL uint64.
 */
public class VdlUint64 extends VdlValue implements Parcelable, TypeAdapterFactory {
    private final long value;

    public VdlUint64(VdlType type, long value) {
        super(type);
        assertKind(Kind.UINT64);
        this.value = value;
    }

    public VdlUint64(long value) {
        this(Types.UINT64, value);
    }

    public long getValue() {
        return this.value;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (this.getClass() != obj.getClass()) return false;
        final VdlUint64 other = (VdlUint64) obj;
        return this.value == other.value;
    }

    @Override
    public int hashCode() {
        return Long.valueOf(value).hashCode();
    }

    @Override
    public String toString() {
        return Long.toString(value);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeLong(value);
    }

    public static final Creator<VdlUint64> CREATOR = new Creator<VdlUint64>() {
        @Override
        public VdlUint64 createFromParcel(Parcel in) {
            return new VdlUint64(in.readLong());
        }

        @Override
        public VdlUint64[] newArray(int size) {
            return new VdlUint64[size];
        }
    };

    protected VdlUint64(VdlType type) {
        this(type, 0);
    }

    public VdlUint64() {
        this(0);
    }

    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        if (!type.equals(new TypeToken<VdlUint64>() {})) {
            return null;
        }
        final TypeAdapter<Long> delegate = gson.getAdapter(new TypeToken<Long>() {});
        return new TypeAdapter<T>() {
            @Override
            public void write(JsonWriter out, T value) throws IOException {
                delegate.write(out, ((VdlUint64) value).getValue());
            }

            @SuppressWarnings("unchecked")
            @Override
            public T read(JsonReader in) throws IOException {
                return (T) new VdlUint64(delegate.read(in));
            }
        };
    }
}
