package io.veyron.veyron.veyron2.vdl;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * VdlUint64 is a representation of a VDL uint64.
 */
public class VdlUint64 extends VdlValue implements Parcelable {
    private final long value;

    public VdlUint64(VdlType type, long value) {
        super(type);
        assertKind(Kind.UINT64);
        this.value = value;
    }

    public VdlUint64(long value) {
        this(Types.UINT64, value);
    }

    public long getValue() {
        return this.value;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof VdlUint64)) return false;
        final VdlUint64 other = (VdlUint64) obj;
        return value == other.value;
    }

    @Override
    public int hashCode() {
        return Long.valueOf(value).hashCode();
    }

    @Override
    public String toString() {
        return Long.toString(value);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeLong(value);
    }

    public static final Creator<VdlUint64> CREATOR = new Creator<VdlUint64>() {
        @Override
        public VdlUint64 createFromParcel(Parcel in) {
            return new VdlUint64(in.readLong());
        }

        @Override
        public VdlUint64[] newArray(int size) {
            return new VdlUint64[size];
        }
    };

    protected VdlUint64(VdlType type) {
        this(type, 0);
    }

    public VdlUint64() {
        this(0);
    }
}
