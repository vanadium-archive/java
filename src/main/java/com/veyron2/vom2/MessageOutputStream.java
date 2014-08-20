// TODO(bprosnitz) Either finish this or remove it before the 0.1 release.

package com.veyron2.vom2;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import com.veyron2.vdl.Kind;
import com.veyron2.vdl.Type;

/**
 * MessageOutputStream assists in the VOM-encoding process by handling
 * prefixing messages by type and length.
 */
final class MessageOutputStream extends OutputStream {
    private final OutputStream innerOutputStream;
    private final RawVomWriter formattedWriter;
    private final ByteArrayOutputStream bos;
    private boolean messageStarted = false; // The message is currently active.
    private boolean initialized = false; // The stream is initialized (magic
                                         // byte sent).

    public MessageOutputStream(OutputStream os) {
        innerOutputStream = os;
        formattedWriter = new RawVomWriter(innerOutputStream);
        bos = new ByteArrayOutputStream();
    }

    /**
     * Start a new VOM message with the specified type ID and initialize the
     * stream if necessary.
     *
     * @param rawTypeId The type id.
     * @throws IOException
     */
    public void startMessage(long rawTypeId) throws IOException {
        if (messageStarted) {
            throw new RuntimeException("Cannot start chunk twice before finishing");
        }
        if (!initialized) {
            initialized = true;
            // Write the magic byte to begin the stream;
            innerOutputStream.write(0x80);
        }
        messageStarted = true;
        formattedWriter.writeInt(rawTypeId);
    }

    @Override
    public void close() throws IOException {
        if (messageStarted) {
            throw new RuntimeException("Cannot close in the middle of a message");
        }
        innerOutputStream.close();
    }

    @Override
    public void flush() throws IOException {
        innerOutputStream.flush();
    }

    @Override
    public void write(byte[] bytes) throws IOException {
        if (!messageStarted) {
            throw new RuntimeException("Must start before writing chunk");
        }
        bos.write(bytes);
    }

    @Override
    public void write(byte[] bytes, int off, int len) throws IOException {
        if (!messageStarted) {
            throw new RuntimeException("Must start before writing chunk");
        }
        bos.write(bytes, off, len);
    }

    @Override
    public void write(int oneByte) throws IOException {
        if (!messageStarted) {
            throw new RuntimeException("Must start before writing chunk");
        }
        bos.write(oneByte);
    }

    public void finishMessage(Type type) throws IOException {
        if (!messageStarted) {
            throw new RuntimeException("Cannot finish before starting");
        }
        switch (type.getKind()) {
            case LIST:
            case ARRAY:
                if (type.getElem().getKind() == Kind.BYTE) {
                    break;
                }
            case SET:
            case MAP:
            case STRUCT:
            case ANY:
            case ONE_OF:
            case COMPLEX64:
            case COMPLEX128:
                formattedWriter.writeUint(bos.size());
                break;
            default:
                break;
        }
        innerOutputStream.write(bos.toByteArray());
        bos.reset();
        messageStarted = false;
    }

}
