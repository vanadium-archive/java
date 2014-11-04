package io.veyron.veyron.veyron2.vdl;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * VdlComplex64 is a representation of a VDL complex64.
 */
public class VdlComplex64 extends VdlValue implements Parcelable {
    private final float real;
    private final float imag;

    public VdlComplex64(float real, float imag) {
        super(Types.COMPLEX64);
        this.real = real;
        this.imag = imag;
    }

    public VdlComplex64(float real) {
        this(real, 0);
    }

    public float getReal() {
        return real;
    }

    public float getImag() {
        return imag;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (this.getClass() != obj.getClass()) return false;
        final VdlComplex64 other = (VdlComplex64) obj;
        return real == other.real && imag == other.imag;
    }

    @Override
    public int hashCode() {
        return Float.valueOf(real).hashCode() ^ Float.valueOf(imag).hashCode();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeFloat(real);
        out.writeFloat(imag);
    }

    public static final Creator<VdlComplex64> CREATOR = new Creator<VdlComplex64>() {
        @Override
        public VdlComplex64 createFromParcel(Parcel in) {
            return new VdlComplex64(in);
        }

        @Override
        public VdlComplex64[] newArray(int size) {
            return new VdlComplex64[size];
        }
    };

    private VdlComplex64(Parcel in) {
        this(in.readFloat(), in.readFloat());
    }
}
