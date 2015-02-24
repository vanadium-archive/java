package io.v.v23.vdl;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * VdlInt64 is a representation of a VDL int64.
 */
public class VdlInt64 extends VdlValue implements Parcelable {
    private final long value;

    public VdlInt64(VdlType type, long value) {
        super(type);
        assertKind(Kind.INT64);
        this.value = value;
    }

    public VdlInt64(long value) {
        this(Types.INT64, value);
    }

    public long getValue() {
        return this.value;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof VdlInt64)) return false;
        final VdlInt64 other = (VdlInt64) obj;
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

    public static final Creator<VdlInt64> CREATOR = new Creator<VdlInt64>() {
        @Override
        public VdlInt64 createFromParcel(Parcel in) {
            return new VdlInt64(in.readLong());
        }

        @Override
        public VdlInt64[] newArray(int size) {
            return new VdlInt64[size];
        }
    };

    protected VdlInt64(VdlType type) {
        this(type, 0);
    }

    public VdlInt64() {
        this(0);
    }
}
