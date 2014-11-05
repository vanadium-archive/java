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
 * VdlInt16 is a representation of a VDL int16.
 */
public class VdlInt16 extends VdlValue implements Parcelable, TypeAdapterFactory {
    private final short value;

    public VdlInt16(short value) {
        super(Types.INT16);
        this.value = value;
    }

    public short getValue() {
        return this.value;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (this.getClass() != obj.getClass()) return false;
        final VdlInt16 other = (VdlInt16) obj;
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

    public static final Creator<VdlInt16> CREATOR = new Creator<VdlInt16>() {
        @Override
        public VdlInt16 createFromParcel(Parcel in) {
            return new VdlInt16(in);
        }

        @Override
        public VdlInt16[] newArray(int size) {
            return new VdlInt16[size];
        }
    };

    private VdlInt16(Parcel in) {
        this((short) in.readInt());
    }

    public VdlInt16() {
        this((short) 0);
    }

    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        if (!type.equals(new TypeToken<VdlInt16>() {})) {
            return null;
        }
        final TypeAdapter<Short> delegate = gson.getAdapter(new TypeToken<Short>() {});
        return new TypeAdapter<T>() {
            @Override
            public void write(JsonWriter out, T value) throws IOException {
                delegate.write(out, ((VdlInt16) value).getValue());
            }

            @SuppressWarnings("unchecked")
            @Override
            public T read(JsonReader in) throws IOException {
                return (T) new VdlInt16(delegate.read(in));
            }
        };
    }
}