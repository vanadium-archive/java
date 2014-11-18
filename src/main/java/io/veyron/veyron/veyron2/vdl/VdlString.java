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
 * VdlString is a representation of a VDL string.
 */
public class VdlString extends VdlValue implements Parcelable, TypeAdapterFactory {
    private final String value;

    public VdlString(VdlType type, String value) {
        super(type);
        assertKind(Kind.STRING);
        this.value = value;
    }

    public VdlString(String value) {
        this(Types.STRING, value);
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (this.getClass() != obj.getClass()) return false;
        final VdlString other = (VdlString) obj;
        return value.equals(other.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(value);
    }

    public static final Creator<VdlString> CREATOR = new Creator<VdlString>() {
        @Override
        public VdlString createFromParcel(Parcel in) {
            return new VdlString(in.readString());
        }

        @Override
        public VdlString[] newArray(int size) {
            return new VdlString[size];
        }
    };

    protected VdlString(VdlType type) {
        this(type, "");
    }

    public VdlString() {
        this("");
    }

    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        if (!type.equals(new TypeToken<VdlString>() {})) {
            return null;
        }
        final TypeAdapter<String> delegate = gson.getAdapter(new TypeToken<String>() {});
        return new TypeAdapter<T>() {
            @Override
            public void write(JsonWriter out, T value) throws IOException {
                delegate.write(out, ((VdlString) value).getValue());
            }

            @SuppressWarnings("unchecked")
            @Override
            public T read(JsonReader in) throws IOException {
                return (T) new VdlString(delegate.read(in));
            }
        };
    }
}
