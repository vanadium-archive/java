package io.veyron.veyron.veyron2.vom2;

import io.veyron.veyron.veyron2.vdl.Kind;
import io.veyron.veyron.veyron2.vdl.VdlType;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

/**
 * Binary encoding and decoding routines.
 */
final class BinaryUtil {
    /**
     * Every binary stream starts with this magic byte, to distinguish the binary encoding from
     * the JSON encoding. Note that every valid JSON encoding must start with an ASCII character,or
     * the BOM U+FEFF, and this magic byte is unambiguous regardless of the endianness of the JSON
     * encoding.
     */
    public static final byte BINARY_MAGIC_BYTE = (byte) 0x80;

    /**
     * Unsigned integers are the basis for all other primitive values. This is a two-state encoding.
     * If the number is less than 128 (0 through 0x7f), its value is written directly. Otherwise the
     * value is written in big-endian byte order preceded by the negated byte length.
     */
    public static void encodeUint(OutputStream out, final long value) throws IOException {
        if ((value & 0x7f) == value) {
            out.write((byte) value);
            return;
        }
        int len = 0;
        while ((value >>> (len * 8)) > 0xff) {
            len++;
        }
        len++;
        out.write(-len);
        while (len > 0) {
            len--;
            out.write((byte) (value >>> (len * 8)));
        }
    }

    public static void encodeUint(OutputStream out, final int value) throws IOException {
        encodeUint(out, ((long) value) & 0xffffffff);
    }

    public static void encodeUint(OutputStream out, final short value) throws IOException {
        encodeUint(out, ((long) value) & 0xffff);
    }

    /**
     * Signed integers are encoded as unsigned integers, where the low bit says whether to
     * complement the other bits to recover the int.
     */
    public static void encodeInt(OutputStream out, final long value) throws IOException {
        if (value < 0) {
            encodeUint(out, ((~value) << 1) | 1);
        } else {
            encodeUint(out, value << 1);
        }
    }

    /**
     * Floating point numbers are encoded as byte-reversed ieee754.
     */
    public static void encodeDouble(OutputStream out, final double value) throws IOException {
        encodeUint(out, Long.reverseBytes(Double.doubleToLongBits(value)));
    }

    /**
     * Booleans are encoded as a byte where 0 = false and anything else is true.
     */
    public static void encodeBoolean(OutputStream out, final boolean value) throws IOException {
        out.write(value ? 1 : 0);
    }

    /**
     * Strings are encoded as the byte count followed by uninterpreted bytes.
     */
    public static void encodeString(OutputStream out, String value) throws IOException {
        if (value == null) {
            value = "";
        }
        byte[] data = value.getBytes(Charset.forName("UTF-8"));
        encodeUint(out, data.length);
        out.write(data);
    }

    /**
     * Returns true iff the type is encoded with a top-level message length.
     */
    public static boolean hasBinaryMsgLen(VdlType type) {
        if ((type.getKind() == Kind.ARRAY || type.getKind() == Kind.LIST)
                && type.getElem().getKind() == Kind.BYTE) {
            return false;
        }

        switch (type.getKind()) {
            case ANY:
            case ARRAY:
            case COMPLEX64:
            case COMPLEX128:
            case LIST:
            case MAP:
            case ONE_OF:
            case SET:
            case STRUCT:
                return true;
            default:
                return false;
        }
    }
}
