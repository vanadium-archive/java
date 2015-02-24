package io.v.v23.vdl;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * VdlString is a representation of a VDL string.
 */
public class VdlString extends VdlValue implements Parcelable {
    private final String value;

    public VdlString(VdlType type, String value) {
        super(type);
        assertKind(Kind.STRING);
        this.value = value;
    }

    public VdlString(String value) {
        this(Types.STRING, value);
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof VdlString)) return false;
        final VdlString other = (VdlString) obj;
        return value.equals(other.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(value);
    }

    public static final Creator<VdlString> CREATOR = new Creator<VdlString>() {
        @Override
        public VdlString createFromParcel(Parcel in) {
            return new VdlString(in.readString());
        }

        @Override
        public VdlString[] newArray(int size) {
            return new VdlString[size];
        }
    };

    protected VdlString(VdlType type) {
        this(type, "");
    }

    public VdlString() {
        this("");
    }
}
