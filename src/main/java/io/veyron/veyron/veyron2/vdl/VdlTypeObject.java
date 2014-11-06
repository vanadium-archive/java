package io.veyron.veyron.veyron2.vdl;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * VdlTypeObject is a representation of a VDL typeObject.
 */
public final class VdlTypeObject extends VdlValue implements Parcelable {
    private final VdlType typeObject;

    public VdlTypeObject(VdlType typeObject) {
        super(Types.TYPEOBJECT);
        this.typeObject = typeObject;
    }

    public VdlType getTypeObject() {
        return typeObject;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (this.getClass() != obj.getClass()) return false;
        final VdlTypeObject other = (VdlTypeObject) obj;
        return typeObject.equals(other.typeObject);
    }

    @Override
    public int hashCode() {
        return typeObject.hashCode();
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

    public static final Creator<VdlTypeObject> CREATOR = new Creator<VdlTypeObject>() {
        @Override
        public VdlTypeObject createFromParcel(Parcel in) {
            // TODO(rogulenko): replace this with vom decoding
            return (VdlTypeObject) in.readSerializable();
        }

        @Override
        public VdlTypeObject[] newArray(int size) {
            return new VdlTypeObject[size];
        }
    };
}
