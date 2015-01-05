package io.v.core.veyron2.vdl;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * VdlFloat64 is a representation of a VDL float64.
 */
public class VdlFloat64 extends VdlValue implements Parcelable {
    private final double value;

    public VdlFloat64(VdlType type, double value) {
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
        if (!(obj instanceof VdlFloat64)) return false;
        final VdlFloat64 other = (VdlFloat64) obj;
        return value == other.value;
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
}
