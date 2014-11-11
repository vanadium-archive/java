package io.veyron.veyron.veyron2.vdl;

import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.IOException;

/**
 * VdlUint32 is a representation of a VDL uint32.
 */
public class VdlUint32 extends VdlValue implements Parcelable, TypeAdapterFactory {
    private final int value;

    protected VdlUint32(VdlType type, int value) {
        super(type);
        assertKind(Kind.UINT32);
        this.value = value;
    }

    public VdlUint32(int value) {
        this(Types.UINT32, value);
    }

    public int getValue() {
        return value;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (this.getClass() != obj.getClass()) return false;
        final VdlUint32 other = (VdlUint32) obj;
        return this.value == other.value;
    }

    @Override
    public int hashCode() {
        return value;
    }

    @Override
    public String toString() {
        return Integer.toString(value);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(value);
    }

    public static final Creator<VdlUint32> CREATOR = new Creator<VdlUint32>() {
        @Override
        public VdlUint32 createFromParcel(Parcel in) {
            return new VdlUint32(in.readInt());
        }

        @Override
        public VdlUint32[] newArray(int size) {
            return new VdlUint32[size];
        }
    };

    protected VdlUint32(VdlType type) {
        this(type, 0);
    }

    public VdlUint32() {
        this(0);
    }

    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        if (!type.equals(new TypeToken<VdlUint32>() {})) {
            return null;
        }
        final TypeAdapter<Integer> delegate = gson.getAdapter(new TypeToken<Integer>() {});
        return new TypeAdapter<T>() {
            @Override
            public void write(JsonWriter out, T value) throws IOException {
                delegate.write(out, ((VdlUint32) value).getValue());
            }

            @SuppressWarnings("unchecked")
            @Override
            public T read(JsonReader in) throws IOException {
                return (T) new VdlUint32(delegate.read(in));
            }
        };
    }
}
