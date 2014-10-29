// TODO(bprosnitz) Either finish this or remove it before the 0.1 release.
// TODO(bprosnitz) Any and one of not currently supported.

package io.veyron.veyron.veyron2.vom2;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Stack;

import org.apache.commons.math3.complex.Complex;

import io.veyron.veyron.veyron2.vdl.Kind;
import io.veyron.veyron.veyron2.vdl.VdlType;

/**
 * Decodes from VOM-encoded stream.
 */
final class Decoder {
    private final RawVomReader reader;
    private final TypeDecoder typeCache;
    private boolean firstMessage = true;

    // The top of the type stack points to the element type that is expected to
    // be seen next,
    // based on the stack type of the parent objects. If there is no parent
    // object, this is empty.
    // Note that this differs from the Encoder.
    private final Stack<VdlType> typeStack = new Stack<VdlType>();

    public Decoder(InputStream is) {
        reader = new RawVomReader(is);
        typeCache = new TypeDecoder();
    }

    public boolean readBool() throws IOException, CorruptVomStreamException {
        startReadKind(false, Kind.BOOL);
        return reader.readBoolean();
    }

    public String readString() throws IOException, CorruptVomStreamException {
        startReadKind(false, Kind.STRING);
        return reader.readString();
    }

    public long readUint64() throws CorruptVomStreamException, IOException,
            ConversionException {
        return readNumber(Kind.UINT64, Long.class);
    }

    public int readUint32() throws CorruptVomStreamException, IOException,
            ConversionException {
        return readNumber(Kind.UINT32, Integer.class);
    }

    public short readUint16() throws CorruptVomStreamException, IOException,
            ConversionException {
        return readNumber(Kind.UINT16, Short.class);
    }

    public long readInt64() throws CorruptVomStreamException, IOException,
            ConversionException {
        return readNumber(Kind.INT64, Long.class);
    }

    public int readInt32() throws CorruptVomStreamException, IOException,
            ConversionException {
        return readNumber(Kind.INT32, Integer.class);
    }

    public short readInt16() throws CorruptVomStreamException, IOException,
            ConversionException {
        return readNumber(Kind.INT16, Short.class);
    }

    public double readFloat64() throws CorruptVomStreamException, IOException,
            ConversionException {
        return readNumber(Kind.FLOAT64, Double.class);
    }

    public float readFloat32() throws CorruptVomStreamException, IOException,
            ConversionException {
        return readNumber(Kind.FLOAT32, Float.class);
    }

    public Complex readComplex128() throws CorruptVomStreamException,
            IOException, ConversionException {
        return readNumber(Kind.COMPLEX128, Complex.class);
    }

    public Complex readComplex64() throws CorruptVomStreamException,
            IOException, ConversionException {
        return readNumber(Kind.COMPLEX64, Complex.class);
    }

    public void arrayStart() throws UndefinedTypeIdException,
            UnexpectedKindException, IOException {
        startReadKind(true, Kind.ARRAY);
        typeStack.push(typeStack.peek().getElem());
    }

    public void arrayEnd() {
        endComposite(Kind.ARRAY);
    }

    public int listStart() throws CorruptVomStreamException, IOException {
        startReadKind(true, Kind.LIST);
        typeStack.push(typeStack.peek().getElem());
        return (int) reader.readUint();
    }

    public void listEnd() {
        endComposite(Kind.LIST);
    }

    public int setStart() throws CorruptVomStreamException, IOException {
        startReadKind(true, Kind.SET);
        typeStack.push(typeStack.peek().getKey());
        return (int) reader.readUint();
    }

    public void setEnd() {
        endComposite(Kind.SET);
    }

    public int mapStart() throws CorruptVomStreamException, IOException {
        startReadKind(true, Kind.MAP);
        typeStack.push(null);
        return (int) reader.readUint();
    }

    public void mapStartKey() {
        VdlType elemType = typeStack.pop();
        if (elemType != null && typeStack.peek().getElem() != elemType) {
            throw new RuntimeException(
                    "Start key expected elem or null to be previous type");
        }
        typeStack.push(typeStack.peek().getKey());
    }

    public void mapEndKeyStartElem() {
        VdlType keyType = typeStack.pop();
        if (typeStack.peek().getKey() != keyType) {
            throw new RuntimeException(
                    "Start elem expected key to be previous type.");
        }
        typeStack.push(typeStack.peek().getElem());
    }

    public void mapEnd() {
        endComposite(Kind.MAP);
    }

    public void structStart() throws UndefinedTypeIdException,
            UnexpectedKindException, IOException {
        startReadKind(true, Kind.STRUCT);
        typeStack.push(null);
    }

    public String structNextField() throws CorruptVomStreamException,
            IOException {
        int index = (int) reader.readUint() - 1; // Zero-based index (-1 means
                                                 // end of struct message).
        if (index == -1) {
            return null;
        }
        typeStack.pop();
        VdlType structType = typeStack.peek();
        typeStack.push(structType.getFields()[index].getType());
        return structType.getFields()[index].getName();
    }

    public void structEnd() {
        endComposite(Kind.STRUCT);
    }

    private long kindToBits(Kind k) {
        switch (k) {
            case UINT16:
                return 16;
            case UINT32:
                return 32;
            case UINT64:
                return 64;
            case INT16:
                return 16;
            case INT32:
                return 32;
            case INT64:
                return 64;
            case FLOAT32:
                return 32;
            case FLOAT64:
                return 64;
            case COMPLEX64:
                return 64;
            case COMPLEX128:
                return 128;
            default:
                throw new RuntimeException("Cannot get bits of non numeric kind");
        }
    }

    private <T> T convertLong(long val, Class<T> klass) {
        if (klass.equals(Short.class)) {
            return klass.cast((short) val);
        } else if (klass.equals(Integer.class)) {
            return klass.cast((int) val);
        } else {
            return klass.cast(val);
        }
    }

    private <T> T convertFloat(double val, Class<T> klass) {
        if (klass.equals(Float.class)) {
            return klass.cast((float) val);
        } else {
            return klass.cast(val);
        }
    }

    private <T> T readNumber(Kind targetKind, Class<T> klass)
            throws IOException, CorruptVomStreamException, ConversionException {
        Kind actualKind = startReadKind(false, Kind.UINT16, Kind.UINT32,
                Kind.UINT64, Kind.INT16, Kind.INT32, Kind.INT64, Kind.FLOAT32,
                Kind.FLOAT64, Kind.COMPLEX64, Kind.COMPLEX128);
        switch (actualKind) {
            case UINT16:
            case UINT32:
            case UINT64: {
                long val = reader.readUint();
                if (actualKind == Kind.UINT16) {
                    val = val & 0xffffL;
                } else if (actualKind == Kind.UINT32) {
                    val = val & 0xffffffffL;
                }
                switch (targetKind) {
                    case UINT16:
                    case UINT32:
                    case UINT64: {
                        if (NumericConvertability.hasOverflowUint(val,
                                kindToBits(targetKind))) {
                            throw new ConversionException("Failed to convert value "
                                    + val + " of kind " + actualKind + " to "
                                    + targetKind);
                        }
                        return convertLong(val, klass);
                    }
                    case INT16:
                    case INT32:
                    case INT64: {
                        if (!NumericConvertability.canConvertUintToInt(val,
                                kindToBits(targetKind))) {
                            throw new ConversionException("Failed to convert value "
                                    + val + " of kind " + actualKind + " to "
                                    + targetKind);
                        }
                        return convertLong(val, klass);
                    }
                    case FLOAT32:
                    case FLOAT64: {
                        if (!NumericConvertability.canConvertUintToFloat(val,
                                kindToBits(targetKind))) {
                            throw new ConversionException("Failed to convert value "
                                    + val + " of kind " + actualKind + " to "
                                    + targetKind);
                        }
                        return convertFloat((double) val, klass);
                    }
                    case COMPLEX64:
                    case COMPLEX128: {
                        if (!NumericConvertability.canConvertUintToFloat(val,
                                kindToBits(targetKind) / 2)) {
                            throw new ConversionException("Failed to convert value "
                                    + val + " of kind " + actualKind + " to "
                                    + targetKind);
                        }
                        return klass.cast(new Complex(val, 0));
                    }
                    default:
                        break;
                }
                break;
            }
            case INT16:
            case INT32:
            case INT64: {
                long val = reader.readInt();
                switch (targetKind) {
                    case UINT16:
                    case UINT32:
                    case UINT64: {
                        if (!NumericConvertability.canConvertIntToUint(val,
                                kindToBits(targetKind))) {
                            throw new ConversionException("Failed to convert value "
                                    + val + " of kind " + actualKind + " to "
                                    + targetKind);
                        }
                        return convertLong(val, klass);
                    }
                    case INT16:
                    case INT32:
                    case INT64: {
                        if (NumericConvertability.hasOverflowInt(val,
                                kindToBits(targetKind))) {
                            throw new ConversionException("Failed to convert value "
                                    + val + " of kind " + actualKind + " to "
                                    + targetKind);
                        }
                        return convertLong(val, klass);
                    }
                    case FLOAT32:
                    case FLOAT64: {
                        if (!NumericConvertability.canConvertIntToFloat(val,
                                kindToBits(targetKind))) {
                            throw new ConversionException("Failed to convert value "
                                    + val + " of kind " + actualKind + " to "
                                    + targetKind);
                        }
                        return convertFloat((double) val, klass);
                    }
                    case COMPLEX64:
                    case COMPLEX128: {
                        if (!NumericConvertability.canConvertIntToFloat(val,
                                kindToBits(targetKind) / 2)) {
                            throw new ConversionException("Failed to convert value "
                                    + val + " of kind " + actualKind + " to "
                                    + targetKind);
                        }
                        return klass.cast(new Complex(val, 0));
                    }
                    default:
                        break;
                }
                break;
            }
            case FLOAT32:
            case FLOAT64: {
                double val = reader.readFloat();
                switch (targetKind) {
                    case UINT16:
                    case UINT32:
                    case UINT64: {
                        if (!NumericConvertability.canConvertFloatToUint(val,
                                kindToBits(targetKind))) {
                            throw new ConversionException("Failed to convert value "
                                    + val + " of kind " + actualKind + " to "
                                    + targetKind);
                        }
                        return convertLong((long) val, klass);
                    }
                    case INT16:
                    case INT32:
                    case INT64: {
                        if (!NumericConvertability.canConvertFloatToInt(val,
                                kindToBits(targetKind))) {
                            throw new ConversionException("Failed to convert value "
                                    + val + " of kind " + actualKind + " to "
                                    + targetKind);
                        }
                        return convertLong((long) val, klass);
                    }
                    case FLOAT32:
                    case FLOAT64: {
                        return convertFloat(val, klass);
                    }
                    case COMPLEX64:
                    case COMPLEX128: {
                        return klass.cast(new Complex(val, 0));
                    }
                    default:
                        break;
                }
                break;
            }
            case COMPLEX64:
            case COMPLEX128: {
                double real = reader.readFloat();
                double imag = reader.readFloat();
                Complex val = new Complex(real, imag);
                switch (targetKind) {
                    case UINT16:
                    case UINT32:
                    case UINT64: {
                        if (!NumericConvertability.canConvertComplexToUint(val,
                                kindToBits(targetKind))) {
                            throw new ConversionException("Failed to convert value "
                                    + val + " of kind " + actualKind + " to "
                                    + targetKind);
                        }
                        return convertLong((long) val.getReal(), klass);
                    }
                    case INT16:
                    case INT32:
                    case INT64: {
                        if (!NumericConvertability.canConvertComplexToInt(val,
                                kindToBits(targetKind))) {
                            throw new ConversionException("Failed to convert value "
                                    + val + " of kind " + actualKind + " to "
                                    + targetKind);
                        }
                        return convertLong((long) val.getReal(), klass);
                    }
                    case FLOAT32:
                    case FLOAT64: {
                        if (!NumericConvertability.canConvertComplexToFloat(val)) {
                            throw new ConversionException("Failed to convert value "
                                    + val + " of kind " + actualKind + " to "
                                    + targetKind);
                        }
                        return convertFloat(val.getReal(), klass);
                    }
                    case COMPLEX64:
                    case COMPLEX128:
                        return klass.cast(val);
                    default:
                        break;
                }
                break;
            }
            default:
                break;
        }
        throw new RuntimeException("Unexpected conversion: " + actualKind
                + " to " + targetKind);
    }

    private Kind startReadKind(boolean composite, Kind... expectedKinds)
            throws IOException, UndefinedTypeIdException,
            UnexpectedKindException {
        if (firstMessage) {
            byte[] magic = reader.readRawBytes(1);
            if (magic[0] != (byte) 0x80) {
                throw new CorruptVomStreamException("Missing magic number");
            }
            firstMessage = false;
        }
        VdlType type;
        if (typeStack.empty()) {
            long typeId = reader.readInt();
            while (typeId < 0) {
                reader.readUint(); // length of type definition struct
                typeCache.defineType(-typeId, reader);
                typeId = reader.readInt();
            }
            type = typeCache.lookupType(typeId);
            if (type == null) {
                throw new UndefinedTypeIdException(typeId);
            }
            switch (type.getKind()) {
                case BOOL:
                case BYTE:
                case UINT16:
                case UINT32:
                case UINT64:
                case INT16:
                case INT32:
                case INT64:
                case FLOAT32:
                case FLOAT64:
                case STRING:
                    // case Complex64: // TODO(bprosnitz) Make complex act as a
                    // primitive.
                    // case Complex128:
                    break;
                default:
                    if (type.getKind() != Kind.COMPLEX64
                            && type.getKind() != Kind.COMPLEX128) {
                        typeStack.push(type); // push composite type.
                    }
                    reader.readUint(); // composite types send length
                    break;
            }
        } else {
            type = typeStack.peek();
            if (type == null) {
                throw new RuntimeException(
                        "Type stack out of sync. Peeked null");
            }
        }
        for (Kind expectedKind : expectedKinds) {
            if (type.getKind() == expectedKind) {
                return type.getKind();
            }
        }
        throw new UnexpectedKindException(type.getKind(), expectedKinds);
    }

    private void endComposite(Kind expectedCompositeKind) {
        typeStack.pop(); // pop the elem/key type.
        if (typeStack.size() == 0) {
            throw new RuntimeException("Missing composite type");
        }
        if (typeStack.peek().getKind() != expectedCompositeKind) {
            throw new RuntimeException("Type stack out of sync");
        }
        if (typeStack.size() == 1) {
            typeStack.pop(); // reset to 0 elements after finishing the
                             // composite value.
        }
    }

    VdlType currentType() {
        return typeStack.peek();
    }

    public static class UndefinedTypeIdException extends
            CorruptVomStreamException {
        private static final long serialVersionUID = 1L;

        public UndefinedTypeIdException(long typeId) {
            super("Type id " + typeId + " is not defined");
        }
    }

    public static class UnexpectedKindException extends
            CorruptVomStreamException {
        private static final long serialVersionUID = 1L;

        public UnexpectedKindException(Kind seenType, Kind... expectedTypes) {
            super("Kind " + seenType + " unexpected. Expected: "
                    + Arrays.toString(expectedTypes));
        }
    }
}
