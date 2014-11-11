package io.veyron.veyron.veyron2.vdl;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

/**
 * VdlOneOf is a representation of a VDL oneOf.
 */
public class VdlOneOf extends VdlValue implements Parcelable {
    private Serializable value;

    public VdlOneOf(VdlType type) {
        super(type);
        assertKind(Kind.ONE_OF);
    }

    private boolean assignValue(VdlType valueType, Serializable value) {
        for (VdlType type : getType().getTypes()) {
            if (type.equals(valueType)) {
                this.value = value;
                return true;
            }
        }
        this.value = null;
        return false;
    }

    public boolean assignValue(VdlValue value) {
        return assignValue(value.getType(), value);
    }

    public Serializable getValue() {
        return value;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (this.getClass() != obj.getClass()) return false;
        final VdlOneOf other = (VdlOneOf) obj;
        return value.equals(other.value);
    }

    @Override
    public int hashCode() {
        return value == null ? 0 : value.hashCode();
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

    public static final Creator<VdlOneOf> CREATOR = new Creator<VdlOneOf>() {
        @Override
        public VdlOneOf createFromParcel(Parcel in) {
            // TODO(rogulenko): replace this with vom decoding
            return (VdlOneOf) in.readSerializable();
        }

        @Override
        public VdlOneOf[] newArray(int size) {
            return new VdlOneOf[size];
        }
    };
}
