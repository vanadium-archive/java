package io.veyron.veyron.veyron2.vdl;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * VdlEnum is a representation of a VDL enum.
 */
public class VdlEnum extends VdlValue implements Parcelable {
    private final String name;

    public VdlEnum(VdlType type, String name) {
        super(type);
        assertKind(Kind.ENUM);
        if (type.getLabels().indexOf(name) == -1) {
            throw new IllegalArgumentException("Undeclared enum label " + name);
        }
        this.name = name;
    }

    public String name() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof VdlEnum)) return false;
        final VdlEnum other = (VdlEnum) obj;
        return name.equals(other.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        // TODO(rogulenko): replace this with vom encoding
        out.writeSerializable(this);
    }

    public static final Creator<VdlEnum> CREATOR = new Creator<VdlEnum>() {
        @Override
        public VdlEnum createFromParcel(Parcel in) {
            // TODO(rogulenko): replace this with vom decoding
            return (VdlEnum) in.readSerializable();
        }

        @Override
        public VdlEnum[] newArray(int size) {
            return new VdlEnum[size];
        }
    };
}
