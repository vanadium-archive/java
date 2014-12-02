package io.veyron.veyron.veyron2.vdl;

import android.os.Parcel;
import android.os.Parcelable;

import java.lang.reflect.Type;

/**
 * VdlOptional is a representation of a VDL optional.
 *
 * @param <T> The type of the element.
 */
public class VdlOptional<T extends VdlValue> extends VdlValue implements Parcelable {
    private final T elem;

    /**
     * Creates a VdlOptional of provided VDL type that wraps a provided element.
     *
     * @param vdlType the runtime VDL type of VdlOptional
     * @param element the wrapped element
     */
    public VdlOptional(VdlType vdlType, T element) {
        super(vdlType);
        assertKind(Kind.OPTIONAL);
        this.elem = element;
    }

    /**
     * Creates a VdlOptional that wraps a provided non-null element.
     *
     * @param element the wrapped element
     * @throws NullPointerException is the element is null
     */
    public VdlOptional(T element) {
        this(Types.optionalOf(element.vdlType()), element);
    }

    /**
     * Creates a VdlOptional of provided VDL type that wraps null.
     *
     * @param vdlType the runtime VDL type of VdlOptional
     */
    public VdlOptional(VdlType vdlType) {
        this(vdlType, null);
    }

    /**
     * Creates a VdlOptional of provided type that wraps null.
     *
     * @param type the runtime type of VdlOptional
     */
    public VdlOptional(Type type) {
        this(Types.getVdlTypeFromReflect(type));
    }

    public boolean isNull() {
        return elem == null;
    }

    public T getElem() {
        return elem;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (!(obj instanceof VdlOptional)) return false;
        final VdlOptional<?> other = (VdlOptional<?>) obj;
        return elem == null ? other.elem == null : elem.equals(other.elem);
    }

    @Override
    public int hashCode() {
        return elem == null ? 0 : elem.hashCode();
    }

    @Override
    public String toString() {
        return elem == null ? null : elem.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeSerializable(this);
    }

    @SuppressWarnings("rawtypes")
    public static final Creator<VdlOptional> CREATOR = new Creator<VdlOptional>() {
        @Override
        public VdlOptional createFromParcel(Parcel in) {
            return (VdlOptional) in.readSerializable();
        }

        @Override
        public VdlOptional[] newArray(int size) {
            return new VdlOptional[size];
        }
    };
}
