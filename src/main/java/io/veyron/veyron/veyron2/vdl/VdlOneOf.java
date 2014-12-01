package io.veyron.veyron.veyron2.vdl;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

/**
 * VdlOneOf is a representation of a VDL oneOf.
 */
public class VdlOneOf extends VdlValue implements Parcelable {
    private Serializable elem;
    private int index;

    protected VdlOneOf(VdlType type, int index, Serializable elem) {
        super(type);
        assertKind(Kind.ONE_OF);
        if (index < 0 || index > type.getFields().size()) {
            throw new IllegalArgumentException("One of index " + index + " is out of range " + 0 +
                    "..." + (type.getFields().size() - 1));
        }
        this.index = index;
        this.elem = elem;
    }

    public VdlOneOf(VdlType type, int index, VdlType elemType, Serializable elem) {
        this(type, index, elem);
        if (!vdlType().getFields().get(index).getType().equals(elemType)) {
            throw new IllegalArgumentException("Illegal type " + elemType + " of elem: it should"
                    + "be " + vdlType().getFields().get(index).getType());
        }
    }

    public Serializable getElem() {
        return elem;
    }

    public int getIndex() {
        return index;
    }

    public String getName() {
        return vdlType().getFields().get(index).getName();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof VdlOneOf)) return false;
        final VdlOneOf other = (VdlOneOf) obj;
        return getElem().equals(other.getElem());
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
        out.writeSerializable(this);
    }

    public static final Creator<VdlOneOf> CREATOR = new Creator<VdlOneOf>() {
        @Override
        public VdlOneOf createFromParcel(Parcel in) {
            return (VdlOneOf) in.readSerializable();
        }

        @Override
        public VdlOneOf[] newArray(int size) {
            return new VdlOneOf[size];
        }
    };
}
