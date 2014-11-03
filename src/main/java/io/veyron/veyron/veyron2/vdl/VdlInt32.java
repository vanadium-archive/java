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
 * VdlInt32 is a representation of a VDL int32.
 */
public class VdlInt32 extends VdlValue implements Parcelable, TypeAdapterFactory {
    private final int value;

    public VdlInt32(int value) {
        super(Types.INT32);
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (this.getClass() != obj.getClass()) return false;
        final VdlInt32 other = (VdlInt32) obj;
        return this.value == other.value;
    }

    @Override
    public int hashCode() {
        return value;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(value);
    }

    public static final Creator<VdlInt32> CREATOR = new Creator<VdlInt32>() {
        @Override
        public VdlInt32 createFromParcel(Parcel in) {
            return new VdlInt32(in);
        }

        @Override
        public VdlInt32[] newArray(int size) {
            return new VdlInt32[size];
        }
    };

    private VdlInt32(Parcel in) {
        this(in.readInt());
    }

    public VdlInt32() {
        this(0);
    }

    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        if (!type.equals(new TypeToken<VdlInt32>() {})) {
            return null;
        }
        final TypeAdapter<Integer> delegate = gson.getAdapter(new TypeToken<Integer>() {});
        return new TypeAdapter<T>() {
            @Override
            public void write(JsonWriter out, T value) throws IOException {
                delegate.write(out, ((VdlInt32) value).getValue());
            }

            @SuppressWarnings("unchecked")
            @Override
            public T read(JsonReader in) throws IOException {
                return (T) new VdlInt32(delegate.read(in));
            }
        };
    }
}
