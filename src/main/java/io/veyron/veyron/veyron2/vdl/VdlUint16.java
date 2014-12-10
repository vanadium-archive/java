package io.veyron.veyron.veyron2.vdl;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * VdlUint16 is a representation of a VDL uint16.
 */
public class VdlUint16 extends VdlValue implements Parcelable {
    private final short value;

    public VdlUint16(VdlType type, short value) {
        super(type);
        assertKind(Kind.UINT16);
        this.value = value;
    }

    public VdlUint16(short value) {
        this(Types.UINT16, value);
    }

    public short getValue() {
        return this.value;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof VdlUint16)) return false;
        final VdlUint16 other = (VdlUint16) obj;
        return value == other.value;
    }

    @Override
    public int hashCode() {
        return value;
    }

    @Override
    public String toString() {
        return Short.toString(value);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(value);
    }

    public static final Creator<VdlUint16> CREATOR = new Creator<VdlUint16>() {
        @Override
        public VdlUint16 createFromParcel(Parcel in) {
            return new VdlUint16((short) in.readInt());
        }

        @Override
        public VdlUint16[] newArray(int size) {
            return new VdlUint16[size];
        }
    };

    protected VdlUint16(VdlType type) {
        this(type, (short) 0);
    }

    public VdlUint16() {
        this((short) 0);
    }
}
