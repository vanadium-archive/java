package io.v.v23.vdl;

/**
 * VdlComplex128 is a representation of a VDL complex128.
 */
public class VdlComplex128 extends VdlValue {
    private static final long serialVersionUID = 1L;

    private final double real;
    private final double imag;

    public VdlComplex128(VdlType type, double real, double imag) {
        super(type);
        assertKind(Kind.COMPLEX128);
        this.real = real;
        this.imag = imag;
    }

    public VdlComplex128(double real, double imag) {
        this(Types.COMPLEX128, real, imag);
    }

    public VdlComplex128(double real) {
        this(real, 0);
    }

    public VdlComplex128() {
        this(0, 0);
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
        if (!(obj instanceof VdlComplex128)) return false;
        final VdlComplex128 other = (VdlComplex128) obj;
        return real == other.real && imag == other.imag;
    }

    @Override
    public int hashCode() {
        return Double.valueOf(real).hashCode() ^ Double.valueOf(imag).hashCode();
    }

    @Override
    public String toString() {
        return "{real=" + Double.toString(real) + ", imag=" + Double.toString(imag) + "}";
    }
}
