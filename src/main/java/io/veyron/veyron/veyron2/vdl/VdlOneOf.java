package io.veyron.veyron.veyron2.vdl;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;
import java.lang.reflect.Type;

/**
 * VdlOneOf is a representation of a VDL oneOf.
 */
public class VdlOneOf extends VdlValue implements Parcelable {
    private Serializable elem;
    private VdlType elemType;

    public VdlOneOf(VdlType type) {
        super(type);
        assertKind(Kind.ONE_OF);
    }

    private VdlOneOf assignValue(VdlType elemType, Serializable value) {
        for (VdlType type : vdlType().getTypes()) {
            if (type.equals(elemType)) {
                this.elem = value;
                this.elemType = type;
                return this;
            }
        }
        return null;
    }

    /**
     * Tries to assign a value to the {@code VdlOneOf} object. Doesn't modify this object if
     * provided type is incompatible.
     *
     * @param type the runtime type of the value
     * @param value the value to assign
     * @return this {@code VdlOneOf} object or null if the value has incompatible type
     */
    public VdlOneOf assignValue(Type type, Serializable value) {
        return assignValue(Types.getVdlTypeFromReflect(type), value);
    }

    /**
     * Tries to assign a value to the {@code VdlOneOf} object. Doesn't modify this object if
     * provided type is incompatible.
     *
     * @param value the value to assign
     * @return this {@code VdlOneOf} object or null if the value has incompatible type
     */
    public VdlOneOf assignValue(VdlValue value) {
        return assignValue(value.vdlType(), value);
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
        final VdlOneOf other = (VdlOneOf) obj;
        return elem.equals(other.elem);
    }

    @Override
    public int hashCode() {
        return elem == null ? 0 : elem.hashCode();
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
