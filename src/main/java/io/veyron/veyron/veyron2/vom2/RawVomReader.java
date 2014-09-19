// TODO(bprosnitz) Either finish this or remove it before the 0.1 release.

package io.veyron.veyron.veyron2.vom2;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

/**
 * RawVomReader handles low-level vom decoding.
 */
class RawVomReader {
    private final InputStream is;
    private final byte[] longBuffer = new byte[9]; // A performance optimization
                                                   // so that it isn't allocated
                                                   // repeatedly.

    private static final Charset UTF8_CHARSET = Charset.forName("UTF-8");
    private static final String END_OF_STREAM_MESSAGE = "End of stream prematurely reached.";

    public RawVomReader(final InputStream is) {
        this.is = is;
    }

    public long readUint() throws IOException, CorruptVomStreamException {
        final int firstByte = is.read();
        if (firstByte == -1) {
            // EOF.
            throw new CorruptVomStreamException(END_OF_STREAM_MESSAGE);
        }

        if (firstByte <= 0x7f) {
            return firstByte;
        }

        int numBytes = (int) (-(byte) firstByte);
        if (numBytes > 8) {
            throw new CorruptVomStreamException("Invalid long byte length");
        }

        if (is.read(longBuffer, 0, numBytes) == -1) {
            throw new CorruptVomStreamException(END_OF_STREAM_MESSAGE);
        }

        long val = 0;
        for (int i = 0; i < numBytes; i++) {
            val = val << 8 | (longBuffer[i] & 0xff);
        }
        return val;
    }

    public long readInt() throws IOException, CorruptVomStreamException {
        long uint = readUint();
        if ((uint & 1) == 1) {
            return ~(uint >>> 1);
        } else {
            return uint >>> 1;
        }
    }

    public double readFloat() throws IOException, CorruptVomStreamException {
        final long reversed = readUint();
        final long ieee = Long.reverseBytes(reversed);
        return Double.longBitsToDouble(ieee);
    }

    public boolean readBoolean() throws IOException, CorruptVomStreamException {
        int nextByte = is.read();
        if (nextByte == -1) {
            throw new CorruptVomStreamException(END_OF_STREAM_MESSAGE);
        }
        if (nextByte == 1)
            return true;
        if (nextByte == 0)
            return false;
        throw new CorruptVomStreamException("Cannot convert byte " + nextByte + " to boolean");
    }

    static String bytesToHexString(byte[] dat) {
        final StringBuilder builder = new StringBuilder();
        for (byte b : dat) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }

    public byte[] readRawBytes(int length) throws IOException,
            CorruptVomStreamException {
        // TODO(bprosnitz) Upper limit for number of bytes to allocate?
        byte[] buf = new byte[length];
        if (length != 0 && is.read(buf) == -1) {
            throw new CorruptVomStreamException(END_OF_STREAM_MESSAGE);
        }
        return buf;
    }

    public String readString() throws IOException, CorruptVomStreamException {
        int length = (int) readUint();
        String str = new String(readRawBytes(length), UTF8_CHARSET);
        return str;
    }
}
