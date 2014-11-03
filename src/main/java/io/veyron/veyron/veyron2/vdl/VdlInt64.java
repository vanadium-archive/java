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
 * VdlInt64 is a representation of a VDL int64.
 */
public class VdlInt64 extends VdlValue implements Parcelable, TypeAdapterFactory {
    private final long value;

    public VdlInt64(long value) {
        super(Types.INT64);
        this.value = value;
    }

    public long getValue() {
        return this.value;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (this.getClass() != obj.getClass()) return false;
        final VdlInt64 other = (VdlInt64) obj;
        return this.value == other.value;
    }

    @Override
    public int hashCode() {
        return Long.valueOf(value).hashCode();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeLong(value);
    }

    public static final Creator<VdlInt64> CREATOR = new Creator<VdlInt64>() {
        @Override
        public VdlInt64 createFromParcel(Parcel in) {
            return new VdlInt64(in);
        }

        @Override
        public VdlInt64[] newArray(int size) {
            return new VdlInt64[size];
        }
    };

    private VdlInt64(Parcel in) {
        this(in.readLong());
    }

    public VdlInt64() {
        this(0);
    }

    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        if (!type.equals(new TypeToken<VdlInt64>() {})) {
            return null;
        }
        final TypeAdapter<Long> delegate = gson.getAdapter(new TypeToken<Long>() {});
        return new TypeAdapter<T>() {
            @Override
            public void write(JsonWriter out, T value) throws IOException {
                delegate.write(out, ((VdlInt64) value).getValue());
            }

            @SuppressWarnings("unchecked")
            @Override
            public T read(JsonReader in) throws IOException {
                return (T) new VdlInt64(delegate.read(in));
            }
        };
    }
}
