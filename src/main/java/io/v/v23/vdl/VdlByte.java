package io.v.v23.vdl;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * VdlByte is a representation of a VDL byte.
 */
public class VdlByte extends VdlValue implements Parcelable {
    private final byte value;

    public VdlByte(VdlType type, byte value) {
        super(type);
        assertKind(Kind.BYTE);
        this.value = value;
    }

    public VdlByte(byte value) {
        this(Types.BYTE, value);
    }

    public byte getValue() {
        return this.value;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof VdlByte)) return false;
        final VdlByte other = (VdlByte) obj;
        return value == other.value;
    }

    @Override
    public int hashCode() {
        return value;
    }

    @Override
    public String toString() {
        return Byte.toString(value);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeByte(value);
    }

    public static final Creator<VdlByte> CREATOR = new Creator<VdlByte>() {
        @Override
        public VdlByte createFromParcel(Parcel in) {
            return new VdlByte(in.readByte());
        }

        @Override
        public VdlByte[] newArray(int size) {
            return new VdlByte[size];
        }
    };

    protected VdlByte(VdlType type) {
        this(type, (byte) 0);
    }

    public VdlByte() {
        this((byte) 0);
    }
}
