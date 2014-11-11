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
 * VdlFloat64 is a representation of a VDL float64.
 */
public class VdlFloat64 extends VdlValue implements Parcelable, TypeAdapterFactory {
    private final double value;

    protected VdlFloat64(VdlType type, double value) {
        super(type);
        assertKind(Kind.FLOAT64);
        this.value = value;
    }

    public VdlFloat64(double value) {
        this(Types.FLOAT64, value);
    }

    public double getValue() {
        return this.value;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (this.getClass() != obj.getClass()) return false;
        final VdlFloat64 other = (VdlFloat64) obj;
        return this.value == other.value;
    }

    @Override
    public int hashCode() {
        return Double.valueOf(value).hashCode();
    }

    @Override
    public String toString() {
        return Double.toString(value);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeDouble(value);
    }

    public static final Creator<VdlFloat64> CREATOR = new Creator<VdlFloat64>() {
        @Override
        public VdlFloat64 createFromParcel(Parcel in) {
            return new VdlFloat64(in.readDouble());
        }

        @Override
        public VdlFloat64[] newArray(int size) {
            return new VdlFloat64[size];
        }
    };

    protected VdlFloat64(VdlType type) {
        this(type, 0);
    }

    public VdlFloat64() {
        this(0);
    }

    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        if (!type.equals(new TypeToken<VdlFloat64>() {})) {
            return null;
        }
        final TypeAdapter<Double> delegate = gson.getAdapter(new TypeToken<Double>() {});
        return new TypeAdapter<T>() {
            @Override
            public void write(JsonWriter out, T value) throws IOException {
                delegate.write(out, ((VdlFloat64) value).getValue());
            }

            @SuppressWarnings("unchecked")
            @Override
            public T read(JsonReader in) throws IOException {
                return (T) new VdlFloat64(delegate.read(in));
            }
        };
    }
}
