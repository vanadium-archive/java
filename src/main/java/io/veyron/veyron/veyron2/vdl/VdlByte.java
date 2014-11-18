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
 * VdlByte is a representation of a VDL byte.
 */
public class VdlByte extends VdlValue implements Parcelable, TypeAdapterFactory {
    private final byte value;

    public VdlByte(VdlType type, byte value) {
        super(type);
        assertKind(Kind.BYTE);
        this.value = value;
    }

    public VdlByte(byte value) {
        this(Types.BYTE, value);
    }

    public byte getValue() {
        return this.value;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (this.getClass() != obj.getClass()) return false;
        final VdlByte other = (VdlByte) obj;
        return this.value == other.value;
    }

    @Override
    public int hashCode() {
        return value;
    }

    @Override
    public String toString() {
        return Byte.toString(value);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeByte(value);
    }

    public static final Creator<VdlByte> CREATOR = new Creator<VdlByte>() {
        @Override
        public VdlByte createFromParcel(Parcel in) {
            return new VdlByte(in.readByte());
        }

        @Override
        public VdlByte[] newArray(int size) {
            return new VdlByte[size];
        }
    };

    protected VdlByte(VdlType type) {
        this(type, (byte) 0);
    }

    public VdlByte() {
        this((byte) 0);
    }

    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        if (!type.equals(new TypeToken<VdlByte>() {})) {
            return null;
        }
        final TypeAdapter<Byte> delegate = gson.getAdapter(new TypeToken<Byte>() {});
        return new TypeAdapter<T>() {
            @Override
            public void write(JsonWriter out, T value) throws IOException {
                delegate.write(out, ((VdlByte) value).getValue());
            }

            @SuppressWarnings("unchecked")
            @Override
            public T read(JsonReader in) throws IOException {
                return (T) new VdlByte(delegate.read(in));
            }
        };
    }
}
