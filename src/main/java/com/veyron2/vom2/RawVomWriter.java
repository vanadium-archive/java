// TODO(bprosnitz) Either finish this or remove it before the 0.1 release.

package com.veyron2.vom2;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

/**
 * RawVomWriter handles low level VOM encoding.
 */
final class RawVomWriter {
    private final OutputStream os;
    private final byte[] longBuffer = new byte[9]; // A performance optimization
                                                   // so that it isn't allocated
                                                   // repeatedly.

    private static final Charset UTF8_CHARSET = Charset.forName("UTF-8");

    public RawVomWriter(final OutputStream os) {
        this.os = os;
    }

    public void writeUint(final long v) throws IOException {
        if (v < 0 || v > 0xffffffffffffffL) {
            longBuffer[0] = (byte) 0xf8;
            longBuffer[1] = (byte) (v >>> 56);
            longBuffer[2] = (byte) (v >>> 48);
            longBuffer[3] = (byte) (v >>> 40);
            longBuffer[4] = (byte) (v >>> 32);
            longBuffer[5] = (byte) (v >>> 24);
            longBuffer[6] = (byte) (v >>> 16);
            longBuffer[7] = (byte) (v >>> 8);
            longBuffer[8] = (byte) v;
            os.write(longBuffer);
            return;
        }
        if (v <= 0x7fL) {
            os.write((int) v);
        } else if (v <= 0xffL) {
            longBuffer[0] = (byte) 0xff;
            longBuffer[1] = (byte) v;
            os.write(longBuffer, 0, 2);
        } else if (v <= 0xffffL) {
            longBuffer[0] = (byte) 0xfe;
            longBuffer[1] = (byte) (v >>> 8);
            longBuffer[2] = (byte) v;
            os.write(longBuffer, 0, 3);
        } else if (v <= 0xffffffL) {
            longBuffer[0] = (byte) 0xfd;
            longBuffer[1] = (byte) (v >>> 16);
            longBuffer[2] = (byte) (v >>> 8);
            longBuffer[3] = (byte) v;
            os.write(longBuffer, 0, 4);
        } else if (v <= 0xffffffffL) {
            longBuffer[0] = (byte) 0xfc;
            longBuffer[1] = (byte) (v >>> 24);
            longBuffer[2] = (byte) (v >>> 16);
            longBuffer[3] = (byte) (v >>> 8);
            longBuffer[4] = (byte) v;
            os.write(longBuffer, 0, 5);
        } else if (v <= 0xffffffffffL) {
            longBuffer[0] = (byte) 0xfb;
            longBuffer[1] = (byte) (v >>> 32);
            longBuffer[2] = (byte) (v >>> 24);
            longBuffer[3] = (byte) (v >>> 16);
            longBuffer[4] = (byte) (v >>> 8);
            longBuffer[5] = (byte) v;
            os.write(longBuffer, 0, 6);
        } else if (v <= 0xffffffffffffL) {
            longBuffer[0] = (byte) 0xfa;
            longBuffer[1] = (byte) (v >>> 40);
            longBuffer[2] = (byte) (v >>> 32);
            longBuffer[3] = (byte) (v >>> 24);
            longBuffer[4] = (byte) (v >>> 16);
            longBuffer[5] = (byte) (v >>> 8);
            longBuffer[6] = (byte) v;
            os.write(longBuffer, 0, 7);
        } else if (v <= 0xffffffffffffffL) {
            longBuffer[0] = (byte) 0xf9;
            longBuffer[1] = (byte) (v >>> 48);
            longBuffer[2] = (byte) (v >>> 40);
            longBuffer[3] = (byte) (v >>> 32);
            longBuffer[4] = (byte) (v >>> 24);
            longBuffer[5] = (byte) (v >>> 16);
            longBuffer[6] = (byte) (v >>> 8);
            longBuffer[7] = (byte) v;
            os.write(longBuffer, 0, 8);
        } else {
            throw new RuntimeException("Unexpected case");
        }
    }

    public void writeInt(final long v) throws IOException {
        if (v < 0) {
            writeUint(((~v) << 1) | 1);
        } else {
            writeUint(v << 1);
        }
    }

    public void writeFloat(final double d) throws IOException {
        final long ieee = Double.doubleToLongBits(d);
        final long reversed = Long.reverseBytes(ieee);
        writeUint(reversed);
    }

    public void writeBoolean(final boolean v) throws IOException {
        if (v) {
            os.write(1);
        } else {
            os.write(0);
        }
    }

    public void writeRawBytes(final byte[] v) throws IOException {
        if (v == null) {
            // TODO(bprosnitz) Should we convert null to empty byte array?
            throw new RuntimeException("Can only write non-null byte array");
        }
        os.write(v);
    }

    public void writeString(String s) throws IOException {
        if (s == null) {
            s = "";
        }
        byte[] dat = s.getBytes(UTF8_CHARSET);
        writeUint(dat.length);
        writeRawBytes(dat);
    }
}
