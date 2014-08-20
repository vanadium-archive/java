// TODO(bprosnitz) Either finish this or remove it before the 0.1 release.

package com.veyron2.vom2;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.math3.complex.Complex;

import com.veyron2.vdl.Type;

/**
 * RawEncoder provides an interface to encode the components of VOM messages.
 * This is higher level than RawVomWriter but lower level than Encoder.
 * TODO(bprosnitz) Consider removing this.
 */
final class RawEncoder {
    private final RawVomWriter writer;
    private final MessageOutputStream mos;

    public RawEncoder(OutputStream os) {
        mos = new MessageOutputStream(os);
        writer = new RawVomWriter(mos);
    }

    void startTypeMessage(long typeId) throws IOException {
        mos.startMessage(-typeId);
    }

    void startValueMessage(long typeId) throws IOException {
        mos.startMessage(typeId);
    }

    void endMessage(Type type) throws IOException {
        mos.finishMessage(type);
    }

    void writeBool(boolean v) throws IOException {
        writer.writeBoolean(v);
    }

    void writeByte(byte v) throws IOException {
        writer.writeUint(v);
    }

    void writeUint16(short v) throws IOException {
        writer.writeUint(v);
    }

    void writeUint32(int v) throws IOException {
        writer.writeUint(v);
    }

    void writeUint64(long v) throws IOException {
        writer.writeUint(v);
    }

    void writeInt16(short v) throws IOException {
        writer.writeInt(v);
    }

    void writeInt32(int v) throws IOException {
        writer.writeInt(v);
    }

    void writeInt64(long v) throws IOException {
        writer.writeInt(v);
    }

    void writeFloat32(double v) throws IOException {
        writer.writeFloat(v);
    }

    void writeFloat64(double v) throws IOException {
        writer.writeFloat(v);
    }

    void writeComplex64(Complex v) throws IOException {
        writeFloat32(v.getReal());
        writeFloat32(v.getImaginary());
    }

    void writeComplex128(Complex v) throws IOException {
        writeFloat64(v.getReal());
        writeFloat64(v.getImaginary());
    }

    void writeByteArray(byte[] v, int length) throws IOException {
        writer.writeRawBytes(v);
    }

    void writeByteList(byte[] v) throws IOException {
        writer.writeUint(v.length);
        writer.writeRawBytes(v);
    }

    void writeString(String v) throws IOException {
        writer.writeString(v);
    }

    void writeTypeId(long typeId) throws IOException {
        writer.writeUint(typeId);
    }

    void writeNil() {
        throw new RuntimeException("Not yet implemented");
    }

    void writeEnumStart(long len) throws IOException {
        writer.writeUint(len);
    }

    void writeListStart(int len) throws IOException {
        writer.writeUint(len);
    }

    void writeSetStart(int len) throws IOException {
        writer.writeUint(len);
    }

    void writeMapStart(int len) throws IOException {
        writer.writeUint(len);
    }

    void writeNextStructFieldIndex(long index) throws IOException {
        writer.writeUint(index);
    }

    void writeStructEnd() throws IOException {
        writer.writeUint(0);
    }
}
