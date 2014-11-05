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
 * VdlFloat32 is a representation of a VDL float32.
 */
public class VdlFloat32 extends VdlValue implements Parcelable, TypeAdapterFactory {
    private final float value;

    public VdlFloat32(float value) {
        super(Types.FLOAT32);
        this.value = value;
    }

    public float getValue() {
        return this.value;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (this.getClass() != obj.getClass()) return false;
        final VdlFloat32 other = (VdlFloat32) obj;
        return this.value == other.value;
    }

    @Override
    public int hashCode() {
        return Float.valueOf(value).hashCode();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeFloat(value);
    }

    public static final Creator<VdlFloat32> CREATOR = new Creator<VdlFloat32>() {
        @Override
        public VdlFloat32 createFromParcel(Parcel in) {
            return new VdlFloat32(in);
        }

        @Override
        public VdlFloat32[] newArray(int size) {
            return new VdlFloat32[size];
        }
    };

    private VdlFloat32(Parcel in) {
        this(in.readFloat());
    }

    public VdlFloat32() {
        this(0);
    }

    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        if (!type.equals(new TypeToken<VdlFloat32>() {})) {
            return null;
        }
        final TypeAdapter<Float> delegate = gson.getAdapter(new TypeToken<Float>() {});
        return new TypeAdapter<T>() {
            @Override
            public void write(JsonWriter out, T value) throws IOException {
                delegate.write(out, ((VdlFloat32) value).getValue());
            }

            @SuppressWarnings("unchecked")
            @Override
            public T read(JsonReader in) throws IOException {
                return (T) new VdlFloat32(delegate.read(in));
            }
        };
    }
}