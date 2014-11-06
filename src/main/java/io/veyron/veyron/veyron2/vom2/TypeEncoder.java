// TODO(bprosnitz) Either finish this or remove it before the 0.1 release.

package io.veyron.veyron.veyron2.vom2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import io.veyron.veyron.veyron2.vdl.VdlStructField;
import io.veyron.veyron.veyron2.vdl.VdlType;
import io.veyron.veyron.veyron2.vdl.Types;
import io.veyron.veyron.veyron2.wiretype.FieldType;
import io.veyron.veyron.veyron2.wiretype.TypeID;

/**
 * TypeEncoder maintains a mapping of types to type ids and assists in encoding
 * types on the VOM stream.
 */
final class TypeEncoder {
    private final HashMap<VdlType, Long> typeIds = new HashMap<VdlType, Long>();
    private long nextId = 65; // First new type id

    public TypeEncoder() {
    }

    /**
     * encodeType encodes a type if it is not defined and returns the type id.
     *
     * @param enc The encoder to write type definitions to.
     * @param t The type.
     * @return The type id of the provided type.
     * @throws IOException
     */
    public long encodeType(RawEncoder enc, VdlType t) throws IOException {
        // Bootstrap type?
        BootstrapType bt = BootstrapType.findBootstrapType(t);
        if (bt != null) {
            return bt.getId();
        }

        // Already sent type?
        Long id = typeIds.get(t);
        if (id != null) {
            return id;
        }

        // Need to send the type:
        long typeId = nextId++;
        typeIds.put(t, typeId);
        encodeWireType(enc, t, typeId);
        return typeId;
    }

    private void encodeWireType(RawEncoder enc, VdlType t, long typeId) throws IOException {
        switch (t.getKind()) {
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
            case COMPLEX64:
            case COMPLEX128:
            case STRING: {
                long primId = encodeType(enc, Types.PrimitiveTypeFromKind(t.getKind()));
                enc.startTypeMessage(typeId);
                enc.writeUint64(BootstrapType.WIRE_NAMED.getId());
                enc.writeNextStructFieldIndex(1);
                enc.writeString(t.getName());
                enc.writeNextStructFieldIndex(2);
                enc.writeTypeId(primId);
                enc.writeStructEnd();
                enc.endMessage(BootstrapType.WIRE_NAMED.getType());
                break;
            }
            case ENUM: {
                enc.startTypeMessage(typeId);
                enc.writeUint64(BootstrapType.WIRE_ENUM.getId());
                if (t.getName() != null && t.getName() != "") {
                    enc.writeNextStructFieldIndex(1);
                    enc.writeString(t.getName());
                }
                enc.writeNextStructFieldIndex(2);
                enc.writeEnumStart(t.getLabels().size());
                for (String s : t.getLabels()) {
                    enc.writeString(s);
                }
                enc.writeStructEnd();
                enc.endMessage(BootstrapType.WIRE_ENUM.getType());
                break;
            }
            case ARRAY: {
                long elemid = encodeType(enc, t.getElem());
                enc.startTypeMessage(typeId);
                enc.writeUint64(BootstrapType.WIRE_ARRAY.getId());
                if (t.getName() != null && t.getName() != "") {
                    enc.writeNextStructFieldIndex(1);
                    enc.writeString(t.getName());
                }
                enc.writeNextStructFieldIndex(2);
                enc.writeTypeId(elemid);
                enc.writeNextStructFieldIndex(3);
                enc.writeUint64(t.getLength());
                enc.writeStructEnd();
                enc.endMessage(BootstrapType.WIRE_ARRAY.getType());
                break;
            }
            case LIST: {
                long elemid = encodeType(enc, t.getElem());
                enc.startTypeMessage(typeId);
                enc.writeUint64(BootstrapType.WIRE_LIST.getId());
                if (t.getName() != null && t.getName() != "") {
                    enc.writeNextStructFieldIndex(1);
                    enc.writeString(t.getName());
                }
                enc.writeNextStructFieldIndex(2);
                enc.writeTypeId(elemid);
                enc.writeStructEnd();
                enc.endMessage(BootstrapType.WIRE_LIST.getType());
            }
                break;
            case SET: {
                long keyid = encodeType(enc, t.getKey());
                enc.startTypeMessage(typeId);
                enc.writeUint64(BootstrapType.WIRE_SET.getId());
                if (t.getName() != null && t.getName() != "") {
                    enc.writeNextStructFieldIndex(1);
                    enc.writeString(t.getName());
                }
                enc.writeNextStructFieldIndex(2);
                enc.writeTypeId(keyid);
                enc.writeStructEnd();
                enc.endMessage(BootstrapType.WIRE_SET.getType());
            }
                break;
            case MAP: {
                long keyid = encodeType(enc, t.getKey());
                long elemid = encodeType(enc, t.getElem());
                enc.startTypeMessage(typeId);
                enc.writeUint64(BootstrapType.WIRE_MAP.getId());
                if (t.getName() != null && t.getName() != "") {
                    enc.writeNextStructFieldIndex(1);
                    enc.writeString(t.getName());
                }
                enc.writeNextStructFieldIndex(2);
                enc.writeTypeId(keyid);
                enc.writeNextStructFieldIndex(3);
                enc.writeTypeId(elemid);
                enc.writeStructEnd();
                enc.endMessage(BootstrapType.WIRE_MAP.getType());
            }
                break;
            case STRUCT: {
                ArrayList<FieldType> fieldTypes = new ArrayList<FieldType>();
                for (VdlStructField f : t.getFields()) {
                    long fid = encodeType(enc, f.getType());
                    fieldTypes.add(new FieldType(new TypeID(fid), f.getName()));
                }

                enc.startTypeMessage(typeId);
                enc.writeUint64(BootstrapType.WIRE_STRUCT.getId());
                if (t.getName() != null && t.getName() != "") {
                    enc.writeNextStructFieldIndex(1);
                    enc.writeString(t.getName());
                }
                enc.writeNextStructFieldIndex(2);
                enc.writeUint64(fieldTypes.size());
                for (FieldType ft : fieldTypes) {
                    enc.writeNextStructFieldIndex(1);
                    enc.writeString(ft.getName());
                    enc.writeNextStructFieldIndex(2);
                    enc.writeTypeId(ft.getType().getValue());
                    enc.writeStructEnd();
                }
                enc.writeStructEnd();
                enc.endMessage(BootstrapType.WIRE_STRUCT.getType());
            }
                break;
            case ONE_OF: {
                ArrayList<Long> types = new ArrayList<Long>();
                for (VdlType childTy : t.getTypes()) {
                    types.add(encodeType(enc, childTy));
                }

                enc.startTypeMessage(typeId);
                enc.writeUint64(BootstrapType.WIRE_ONE_OF.getId());
                if (t.getName() != null && t.getName() != "") {
                    enc.writeNextStructFieldIndex(1);
                    enc.writeString(t.getName());
                }
                enc.writeNextStructFieldIndex(2);
                enc.writeUint64(types.size());
                for (long l : types) {
                    enc.writeUint64(l);
                }
                enc.writeStructEnd();
                enc.endMessage(BootstrapType.WIRE_ONE_OF.getType());
            }
                break;
            default:
                throw new RuntimeException("Unknown wiretype for kind: " + t.getKind());
        }
    }
}
