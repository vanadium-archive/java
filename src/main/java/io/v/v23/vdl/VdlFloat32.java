package io.v.v23.vdl;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * VdlFloat32 is a representation of a VDL float32.
 */
public class VdlFloat32 extends VdlValue implements Parcelable {
    private final float value;

    public VdlFloat32(VdlType type, float value) {
        super(type);
        assertKind(Kind.FLOAT32);
        this.value = value;
    }

    public VdlFloat32(float value) {
        this(Types.FLOAT32, value);
    }

    public float getValue() {
        return this.value;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof VdlFloat32)) return false;
        final VdlFloat32 other = (VdlFloat32) obj;
        return value == other.value;
    }

    @Override
    public int hashCode() {
        return Float.valueOf(value).hashCode();
    }

    @Override
    public String toString() {
        return Float.toString(value);
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
            return new VdlFloat32(in.readFloat());
        }

        @Override
        public VdlFloat32[] newArray(int size) {
            return new VdlFloat32[size];
        }
    };

    protected VdlFloat32(VdlType type) {
        this(type, 0);
    }

    public VdlFloat32() {
        this(0);
    }
}
