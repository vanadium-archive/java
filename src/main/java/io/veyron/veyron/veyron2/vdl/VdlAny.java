package io.veyron.veyron.veyron2.vdl;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;
import java.lang.reflect.Type;

/**
 * VdlAny is a representation of a VDL any.
 */
public final class VdlAny extends VdlValue implements Parcelable {
    private final Serializable elem;
    private VdlType elemType;

    private VdlAny(VdlType type, Serializable value) {
        super(Types.ANY);
        elem = value;
        elemType = type;
    }

    public VdlAny(Type type, Serializable value) {
        this(Types.getVdlTypeFromReflection(type), value);
    }

    public VdlAny(VdlValue value) {
        this(value.vdlType(), value);
    }

    public Serializable getElem() {
        return elem;
    }

    public VdlType getElemType() {
        return elemType;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (this.getClass() != obj.getClass()) return false;
        final VdlAny other = (VdlAny) obj;
        return elem.equals(other.elem);
    }

    @Override
    public int hashCode() {
        return elem.hashCode();
    }

    @Override
    public String toString() {
        return elem.toString();
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
