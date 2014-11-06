// TODO(bprosnitz) Either finish this or remove it before the 0.1 release.
// TODO(bprosnitz) Any and one of not currently supported.

package io.veyron.veyron.veyron2.vom2;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;

import org.apache.commons.math3.complex.Complex;

import io.veyron.veyron.veyron2.vdl.Kind;
import io.veyron.veyron.veyron2.vdl.VdlStructField;
import io.veyron.veyron.veyron2.vdl.VdlType;

/**
 * Writes VOM-encoded data to a stream.
 */
public final class Encoder {
    private final RawEncoder rawEnc;
    private final TypeEncoder typeEnc;

    // The top of this stack refers to the last element started or written.
    // Note that this differs from the Decoder.
    private final Stack<VdlType> typeStack = new Stack<VdlType>();

    public Encoder(OutputStream os) {
        this.rawEnc = new RawEncoder(os);
        this.typeEnc = new TypeEncoder();
    }

    public void writeBool(boolean v, VdlType t) throws IOException {
        assertKind(t, Kind.BOOL);
        startValueLevel(t);
        rawEnc.writeBool(v);
        endLevel();
    }

    public void writeByte(byte v, VdlType t) throws IOException {
        assertKind(t, Kind.BYTE);
        startValueLevel(t);
        rawEnc.writeByte(v);
        endLevel();
    }

    public void writeUint16(short v, VdlType t) throws IOException {
        assertKind(t, Kind.UINT16);
        startValueLevel(t);
        rawEnc.writeUint16(v);
        endLevel();
    }

    public void writeUint32(int v, VdlType t) throws IOException {
        assertKind(t, Kind.UINT32);
        startValueLevel(t);
        rawEnc.writeUint32(v);
        endLevel();
    }

    public void writeUint64(long v, VdlType t) throws IOException {
        assertKind(t, Kind.UINT64);
        startValueLevel(t);
        rawEnc.writeUint64(v);
        endLevel();
    }

    public void writeInt16(short v, VdlType t) throws IOException {
        assertKind(t, Kind.INT16);
        startValueLevel(t);
        rawEnc.writeInt16(v);
        endLevel();
    }

    public void writeInt32(int v, VdlType t) throws IOException {
        assertKind(t, Kind.INT32);
        startValueLevel(t);
        rawEnc.writeInt32(v);
        endLevel();
    }

    public void writeInt64(long v, VdlType t) throws IOException {
        assertKind(t, Kind.INT64);
        startValueLevel(t);
        rawEnc.writeInt64(v);
        endLevel();
    }

    public void writeFloat32(float v, VdlType t) throws IOException {
        assertKind(t, Kind.FLOAT32);
        startValueLevel(t);
        rawEnc.writeFloat32(v);
        endLevel();
    }

    public void writeFloat64(double v, VdlType t) throws IOException {
        assertKind(t, Kind.FLOAT64);
        startValueLevel(t);
        rawEnc.writeFloat64(v);
        endLevel();
    }

    public void writeComplex64(Complex v, VdlType t) throws IOException {
        assertKind(t, Kind.COMPLEX64);
        startValueLevel(t);
        rawEnc.writeComplex64(v);
        endLevel();
    }

    public void writeComplex128(Complex v, VdlType t) throws IOException {
        assertKind(t, Kind.COMPLEX128);
        startValueLevel(t);
        rawEnc.writeComplex128(v);
        endLevel(); // TODO(bprosnitz) This shouldn't send the msg
                    // length... it is unnecessary.
    }

    public void writeBytes(byte[] v, VdlType t) throws IOException {
        assertKind(t, Kind.ARRAY, Kind.LIST);
        assertKind(t.getElem(), Kind.BYTE);
        startValueLevel(t);
        switch (t.getKind()) {
            case LIST:
                rawEnc.writeByteList(v);
                break;
            case ARRAY:
                rawEnc.writeByteArray(v, t.getLength());
                break;
            default:
                throw new RuntimeException("Invalid kind for writeBytes: " + t.getKind());
        }
        endLevel();
    }

    public void writeString(String v, VdlType t) throws IOException {
        startValueLevel(t);
        rawEnc.writeString(v);
        endLevel();
    }

    public void structStart(VdlType type) throws IOException {
        startValueLevel(type);
    }

    public void structNextField(String name) throws IOException {
        VdlType type = typeStack.peek();
        List<VdlStructField> fields = type.getFields();
        for (int i = 0; i < fields.size(); i++) {
            if (fields.get(i).getName().equals(name)) {
                rawEnc.writeNextStructFieldIndex(i + 1);
                return;
            }
        }
        throw new RuntimeException("Invalid struct field name");
    }

    public void structEnd() throws IOException {
        endLevel();
    }

    public void arrayStart(VdlType type) throws IOException {
        startValueLevel(type);
    }

    public void arrayEnd() throws IOException {
        endLevel();
    }

    public void listStart(int len, VdlType type) throws IOException {
        startValueLevel(type);
        rawEnc.writeListStart(len);
    }

    public void listEnd() throws IOException {
        endLevel();
    }

    public void setStart(int len, VdlType type) throws IOException {
        startValueLevel(type);
        rawEnc.writeSetStart(len);
    }

    public void setEnd() throws IOException {
        endLevel();
    }

    public void mapStart(int len, VdlType type) throws IOException {
        startValueLevel(type);
        rawEnc.writeMapStart(len);
    }

    public void mapEnd() throws IOException {
        endLevel();
    }

    private void startValueLevel(VdlType type) throws IOException {
        // TODO(bprosnitz) For nested dynamic types, this doesn't work because
        // types can't be written to the middle of the stream. We need to either
        // have separate type and value buffers or just not buffer types.
        long typeId = typeEnc.encodeType(rawEnc, type);
        if (typeStack.empty()) {
            rawEnc.startValueMessage(typeId);
        }
        typeStack.push(type);
    }

    private void endLevel() throws IOException {
        VdlType type = typeStack.pop();
        if (typeStack.empty()) {
            rawEnc.endMessage(type);
        }
    }

    private void assertKind(VdlType type, Kind... kinds) {
        for (Kind kind : kinds) {
            if (type.getKind() == kind) {
                return;
            }
        }
        throw new RuntimeException("Invalid kind " + type.getKind() + " expected one of "
                + Arrays.toString(kinds));
    }
}
