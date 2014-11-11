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
 * VdlBool is a representation of a VDL bool.
 */
public class VdlBool extends VdlValue implements Parcelable, TypeAdapterFactory {
    private final boolean value;

    protected VdlBool(VdlType type, boolean value) {
        super(type);
        assertKind(Kind.BOOL);
        this.value = value;
    }

    public VdlBool(boolean value) {
        this(Types.BOOL, value);
    }

    public boolean getValue() {
        return this.value;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (this.getClass() != obj.getClass()) return false;
        final VdlBool other = (VdlBool) obj;
        return this.value == other.value;
    }

    @Override
    public int hashCode() {
        return Boolean.valueOf(value).hashCode();
    }

    @Override
    public String toString() {
        return Boolean.toString(value);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeByte((byte) (value ? 1 : 0));
    }

    public static final Creator<VdlBool> CREATOR = new Creator<VdlBool>() {
        @Override
        public VdlBool createFromParcel(Parcel in) {
            return new VdlBool(in.readByte() == 1);
        }

        @Override
        public VdlBool[] newArray(int size) {
            return new VdlBool[size];
        }
    };

    protected VdlBool(VdlType type) {
        this(type, false);
    }

    public VdlBool() {
        this(false);
    }

    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        if (!type.equals(new TypeToken<VdlBool>() {})) {
            return null;
        }
        final TypeAdapter<Boolean> delegate = gson.getAdapter(new TypeToken<Boolean>() {});
        return new TypeAdapter<T>() {
            @Override
            public void write(JsonWriter out, T value) throws IOException {
                delegate.write(out, ((VdlBool) value).getValue());
            }

            @SuppressWarnings("unchecked")
            @Override
            public T read(JsonReader in) throws IOException {
                return (T) new VdlBool(delegate.read(in));
            }
        };
    }
}
