package io.v.v23.vdl;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * VdlBool is a representation of a VDL bool.
 */
public class VdlBool extends VdlValue implements Parcelable {
    private final boolean value;

    public VdlBool(VdlType type, boolean value) {
        super(type);
        assertKind(Kind.BOOL);
        this.value = value;
    }

    public VdlBool(boolean value) {
        this(Types.BOOL, value);
    }

    public boolean getValue() {
        return this.value;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof VdlBool)) return false;
        final VdlBool other = (VdlBool) obj;
        return value == other.value;
    }

    @Override
    public int hashCode() {
        return Boolean.valueOf(value).hashCode();
    }

    @Override
    public String toString() {
        return Boolean.toString(value);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeByte((byte) (value ? 1 : 0));
    }

    public static final Creator<VdlBool> CREATOR = new Creator<VdlBool>() {
        @Override
        public VdlBool createFromParcel(Parcel in) {
            return new VdlBool(in.readByte() == 1);
        }

        @Override
        public VdlBool[] newArray(int size) {
            return new VdlBool[size];
        }
    };

    protected VdlBool(VdlType type) {
        this(type, false);
    }

    public VdlBool() {
        this(false);
    }
}
