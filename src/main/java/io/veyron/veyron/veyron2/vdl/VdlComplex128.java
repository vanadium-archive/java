package io.veyron.veyron.veyron2.vdl;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * VdlComplex128 is a representation of a VDL complex128.
 */
public class VdlComplex128 extends VdlValue implements Parcelable {
    private final double real;
    private final double imag;

    public VdlComplex128(double real, double imag) {
        super(Types.COMPLEX128);
        this.real = real;
        this.imag = imag;
    }

    public VdlComplex128(double real) {
        this(real, 0);
    }

    public double getReal() {
        return real;
    }

    public double getImag() {
        return imag;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (this.getClass() != obj.getClass()) return false;
        final VdlComplex128 other = (VdlComplex128) obj;
        return real == other.real && imag == other.imag;
    }

    @Override
    public int hashCode() {
        return Double.valueOf(real).hashCode() ^ Double.valueOf(imag).hashCode();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeDouble(real);
        out.writeDouble(imag);
    }

    public static final Creator<VdlComplex128> CREATOR = new Creator<VdlComplex128>() {
        @Override
        public VdlComplex128 createFromParcel(Parcel in) {
            return new VdlComplex128(in);
        }

        @Override
        public VdlComplex128[] newArray(int size) {
            return new VdlComplex128[size];
        }
    };

    private VdlComplex128(Parcel in) {
        this(in.readDouble(), in.readDouble());
    }
}
