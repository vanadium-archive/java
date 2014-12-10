package io.veyron.veyron.veyron2.vdl;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * VdlUint32 is a representation of a VDL uint32.
 */
public class VdlUint32 extends VdlValue implements Parcelable {
    private final int value;

    public VdlUint32(VdlType type, int value) {
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
        if (!(obj instanceof VdlUint32)) return false;
        final VdlUint32 other = (VdlUint32) obj;
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
}
