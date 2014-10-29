// TODO(bprosnitz) Either finish this or remove it before the 0.1 release.

package io.veyron.veyron.veyron.testing;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * BufferedPipedInputStream connects an input and output stream and buffers the
 * input until it is read. This works like PipedInputStream, but does not have a
 * limit on the buffer size.
 */
public class BufferedPipedInputStream extends InputStream {

    private List<byte[]> data;
    private byte[] lastChunk = null;
    private int chunkOffset = 0;
    private boolean eof = false;

    /**
     * BufferedPipedOutputStream is the corresponding output stream to the
     * BufferedPipedInputStream. Writes to this will be placed in the input
     * stream buffer.
     */
    public class BufferedPipedOutputStream extends OutputStream {
        @Override
        public void write(byte[] b, int off, int len) {
            byte[] chunk = new byte[len];
            System.arraycopy(b, off, chunk, 0, len);
            synchronized (data) {
                data.add(chunk);
                data.notify();
            }
        }

        @Override
        public void write(byte[] b) {
            write(b, 0, b.length);
        }

        @Override
        public void write(int b) throws IOException {
            byte[] buf = new byte[] {
                (byte) b
            };
            write(buf);
        }

        @Override
        public void close() {
            synchronized (data) {
                eof = true;
                data.notify();
            }
        }

        @Override
        public void flush() {
        }
    }

    public BufferedPipedInputStream() {
        data = new ArrayList<byte[]>();
    }

    public BufferedPipedOutputStream getOutputStream() {
        return new BufferedPipedOutputStream();
    }

    @Override
    public int available() {
        int count = 0;
        for (byte[] chunk : data) {
            count += chunk.length;
        }
        if (lastChunk != null) {
            count += lastChunk.length - chunkOffset;
        }
        return count;
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    @Override
    public long skip(long n) throws IOException {
        for (int i = 0; i < n; i++) {
            if (read() == -1) {
                return i;
            }
        }
        return n;
    }

    @Override
    public void close() {
        // nop for now
    }

    @Override
    public int read(byte[] b) {
        return read(b, 0, b.length);
    }

    @Override
    public int read(byte[] b, int off, int len) {
        final int origLength = len;
        while (len > 0) {
            synchronized (data) {
                if (lastChunk != null) {
                    int amountLeftInChunk = lastChunk.length - chunkOffset;
                    int copyLength = Math.min(len, amountLeftInChunk);
                    System.arraycopy(lastChunk, chunkOffset, b, off, copyLength);
                    len -= copyLength;
                    off += copyLength;
                    chunkOffset += copyLength;
                    if (chunkOffset >= lastChunk.length) {
                        lastChunk = null;
                    }
                    continue;
                }
                if (data.size() > 0) {
                    lastChunk = data.remove(0);
                    chunkOffset = 0;
                    continue;
                }
                if (eof) {
                    return -1;
                }
                try {
                    data.wait();
                } catch (InterruptedException e) {
                    throw new RuntimeException("Wait unexpectedly interrupted");
                }
            }
        }
        return origLength;
    }

    @Override
    public int read() throws IOException {
        byte[] buf = new byte[1];
        int amt = read(buf);
        if (amt == -1) {
            return -1;
        }
        if (amt != 1) {
            throw new IOException("Invalid read");
        }
        int val = buf[0];
        if (val < 0) {
            val += 256;
        }
        return val;
    }
}
