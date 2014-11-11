package io.veyron.veyron.veyron2.vdl;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * VdlAny is a representation of a VDL any.
 */
public final class VdlAny extends VdlValue implements Parcelable {
    private final VdlValue value;

    public VdlAny(VdlValue value) {
        super(Types.ANY);
        this.value = value;
    }

    public VdlValue getValue() {
        return value;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (this.getClass() != obj.getClass()) return false;
        final VdlAny other = (VdlAny) obj;
        return value.equals(other.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value.toString();
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

    public static final Creator<VdlAny> CREATOR = new Creator<VdlAny>() {
        @Override
        public VdlAny createFromParcel(Parcel in) {
            // TODO(rogulenko): replace this with vom decoding
            return (VdlAny) in.readSerializable();
        }

        @Override
        public VdlAny[] newArray(int size) {
            return new VdlAny[size];
        }
    };
}
