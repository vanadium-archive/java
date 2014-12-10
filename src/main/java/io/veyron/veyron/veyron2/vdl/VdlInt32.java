package io.veyron.veyron.veyron2.vdl;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * VdlInt32 is a representation of a VDL int32.
 */
public class VdlInt32 extends VdlValue implements Parcelable {
    private final int value;

    public VdlInt32(VdlType type, int value) {
        super(type);
        assertKind(Kind.INT32);
        this.value = value;
    }

    public VdlInt32(int value) {
        this(Types.INT32, value);
    }

    public int getValue() {
        return this.value;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof VdlInt32)) return false;
        final VdlInt32 other = (VdlInt32) obj;
        return value == other.value;
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

    public static final Creator<VdlInt32> CREATOR = new Creator<VdlInt32>() {
        @Override
        public VdlInt32 createFromParcel(Parcel in) {
            return new VdlInt32(in.readInt());
        }

        @Override
        public VdlInt32[] newArray(int size) {
            return new VdlInt32[size];
        }
    };

    protected VdlInt32(VdlType type) {
        this(type, 0);
    }

    public VdlInt32() {
        this(0);
    }
}
