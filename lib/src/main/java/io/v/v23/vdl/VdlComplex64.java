package io.v.v23.vdl;

/**
 * VdlComplex64 is a representation of a VDL complex64.
 */
public class VdlComplex64 extends VdlValue {
    private static final long serialVersionUID = 1L;

    private final float real;
    private final float imag;

    public VdlComplex64(VdlType type, float real, float imag) {
        super(type);
        assertKind(Kind.COMPLEX64);
        this.real = real;
        this.imag = imag;
    }

    public VdlComplex64(float real, float imag) {
        this(Types.COMPLEX64, real, imag);
    }

    public VdlComplex64(float real) {
        this(real, 0);
    }

    public VdlComplex64() {
        this(0, 0);
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
        if (!(obj instanceof VdlComplex64)) return false;
        final VdlComplex64 other = (VdlComplex64) obj;
        return real == other.real && imag == other.imag;
    }

    @Override
    public int hashCode() {
        return Float.valueOf(real).hashCode() ^ Float.valueOf(imag).hashCode();
    }

    @Override
    public String toString() {
        return "{real=" + Float.toString(real) + ", imag=" + Float.toString(imag) + "}";
    }
}
