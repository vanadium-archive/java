package io.veyron.veyron.veyron2.vom2;

import org.apache.commons.math3.complex.Complex;

/**
 * Methods to test for convertability of numeric types
 */
final class NumericConvertability {

    // IEEE 754 represents float64 using 52 bits to represent the mantissa, with
    // an extra implied leading bit. That gives us 53 bits to store integers
    // without overflow - i.e. [0, (2^53)-1]. And since 2^53 is a small power of
    // two, it can also be stored without loss via mantissa=1 exponent=53. Thus
    // we have our max and min values. Ditto for float32, which uses 23 bits
    // with
    // an extra implied leading bit.
    private static long DOUBLE_MAX_LOSSLESS_INTEGER = (1L << 53);
    private static long DOUBLE_MIN_LOSSLESS_INTEGER = -(1L << 53);
    private static long FLOAT_MAX_LOSSLESS_INTEGER = (1L << 24);
    private static long FLOAT_MIN_LOSSLESS_INTEGER = -(1L << 24);

    static boolean hasOverflowUint(long x, long bitlen) {
        long shift = 64 - bitlen;
        return x != (x << shift) >>> shift;
    }

    static boolean hasOverflowInt(long x, long bitlen) {
        long shift = 64 - bitlen;
        return x != (x << shift) >> shift;
    }

    static boolean canConvertUintToInt(long x, long bitlen) {
        return x >= 0 && !hasOverflowInt(x, bitlen);
    }

    static boolean canConvertIntToUint(long x, long bitlen) {
        return x >= 0 && !hasOverflowUint(x, bitlen);
    }

    static boolean canConvertUintToFloat(long x, long bitlen) {
        if (x < 0)
            return false; // These values are too large for either type.
        switch ((int) bitlen) {
            case 32:
                return x <= FLOAT_MAX_LOSSLESS_INTEGER;
            default:
                return x <= DOUBLE_MAX_LOSSLESS_INTEGER;
        }
    }

    static boolean canConvertIntToFloat(long x, long bitlen) {
        switch ((int) bitlen) {
            case 32:
                return FLOAT_MIN_LOSSLESS_INTEGER <= x
                        && x <= FLOAT_MAX_LOSSLESS_INTEGER;
            default:
                return DOUBLE_MIN_LOSSLESS_INTEGER <= x
                        && x <= DOUBLE_MAX_LOSSLESS_INTEGER;
        }
    }

    static boolean canConvertFloatToUint(double x, long bitlen) {
        long intPart = (long) x;
        double fracPart = x - intPart;
        return x >= 0 && fracPart == 0 && x <= (2 * (double) Long.MAX_VALUE)
                && !hasOverflowUint(intPart, bitlen);
    }

    static boolean canConvertFloatToInt(double x, long bitlen) {
        long intPart = (long) x;
        double fracPart = x - intPart;
        return fracPart == 0 && x >= (double) (Long.MIN_VALUE)
                && x <= (double) (Long.MAX_VALUE)
                && !hasOverflowInt(intPart, bitlen);
    }

    static boolean canConvertComplexToUint(Complex x, long bitlen) {
        return x.getImaginary() == 0
                && canConvertFloatToUint(x.getReal(), bitlen);
    }

    static boolean canConvertComplexToInt(Complex x, long bitlen) {
        return x.getImaginary() == 0
                && canConvertFloatToInt(x.getReal(), bitlen);
    }

    static boolean canConvertComplexToFloat(Complex x) {
        return x.getImaginary() == 0;
    }

}
