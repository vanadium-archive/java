package io.veyron.veyron.veyron2.vom2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import io.veyron.veyron.veyron2.vdl.VdlStructField;
import io.veyron.veyron.veyron2.vdl.VdlType;
import io.veyron.veyron.veyron2.vdl.Types;

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
        TypeID bt = BootstrapType.getBootstrapTypeId(t);
        if (bt != null) {
            return bt.getValue();
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
                long primId = encodeType(enc, Types.primitiveTypeFromKind(t.getKind()));
                enc.startTypeMessage(typeId);
                enc.writeUint64(Vom2Constants.WIRE_NAMED_ID.getValue());
                enc.writeNextStructFieldIndex(1);
                enc.writeString(t.getName());
                enc.writeNextStructFieldIndex(2);
                enc.writeTypeId(primId);
                enc.writeStructEnd();
                enc.endMessage(WireNamed.VDL_TYPE);
                break;
            }
            case ENUM: {
                enc.startTypeMessage(typeId);
                enc.writeUint64(Vom2Constants.WIRE_ENUM_ID.getValue());
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
                enc.endMessage(WireEnum.VDL_TYPE);
                break;
            }
            case ARRAY: {
                long elemid = encodeType(enc, t.getElem());
                enc.startTypeMessage(typeId);
                enc.writeUint64(Vom2Constants.WIRE_ARRAY_ID.getValue());
                if (t.getName() != null && t.getName() != "") {
                    enc.writeNextStructFieldIndex(1);
                    enc.writeString(t.getName());
                }
                enc.writeNextStructFieldIndex(2);
                enc.writeTypeId(elemid);
                enc.writeNextStructFieldIndex(3);
                enc.writeUint64(t.getLength());
                enc.writeStructEnd();
                enc.endMessage(WireArray.VDL_TYPE);
                break;
            }
            case LIST: {
                long elemid = encodeType(enc, t.getElem());
                enc.startTypeMessage(typeId);
                enc.writeUint64(Vom2Constants.WIRE_LIST_ID.getValue());
                if (t.getName() != null && t.getName() != "") {
                    enc.writeNextStructFieldIndex(1);
                    enc.writeString(t.getName());
                }
                enc.writeNextStructFieldIndex(2);
                enc.writeTypeId(elemid);
                enc.writeStructEnd();
                enc.endMessage(WireList.VDL_TYPE);
            }
                break;
            case SET: {
                long keyid = encodeType(enc, t.getKey());
                enc.startTypeMessage(typeId);
                enc.writeUint64(Vom2Constants.WIRE_SET_ID.getValue());
                if (t.getName() != null && t.getName() != "") {
                    enc.writeNextStructFieldIndex(1);
                    enc.writeString(t.getName());
                }
                enc.writeNextStructFieldIndex(2);
                enc.writeTypeId(keyid);
                enc.writeStructEnd();
                enc.endMessage(WireSet.VDL_TYPE);
            }
                break;
            case MAP: {
                long keyid = encodeType(enc, t.getKey());
                long elemid = encodeType(enc, t.getElem());
                enc.startTypeMessage(typeId);
                enc.writeUint64(Vom2Constants.WIRE_MAP_ID.getValue());
                if (t.getName() != null && t.getName() != "") {
                    enc.writeNextStructFieldIndex(1);
                    enc.writeString(t.getName());
                }
                enc.writeNextStructFieldIndex(2);
                enc.writeTypeId(keyid);
                enc.writeNextStructFieldIndex(3);
                enc.writeTypeId(elemid);
                enc.writeStructEnd();
                enc.endMessage(WireMap.VDL_TYPE);
            }
                break;
            case STRUCT: {
                ArrayList<WireField> fieldTypes = new ArrayList<WireField>();
                for (VdlStructField f : t.getFields()) {
                    long fid = encodeType(enc, f.getType());
                    fieldTypes.add(new WireField(f.getName(), new TypeID(fid)));
                }

                enc.startTypeMessage(typeId);
                enc.writeUint64(Vom2Constants.WIRE_STRUCT_ID.getValue());
                if (t.getName() != null && t.getName() != "") {
                    enc.writeNextStructFieldIndex(1);
                    enc.writeString(t.getName());
                }
                enc.writeNextStructFieldIndex(2);
                enc.writeUint64(fieldTypes.size());
                for (WireField ft : fieldTypes) {
                    enc.writeNextStructFieldIndex(1);
                    enc.writeString(ft.getName());
                    enc.writeNextStructFieldIndex(2);
                    enc.writeTypeId(ft.getType().getValue());
                    enc.writeStructEnd();
                }
                enc.writeStructEnd();
                enc.endMessage(WireStruct.VDL_TYPE);
            }
                break;
            case ONE_OF: {
                ArrayList<Long> types = new ArrayList<Long>();
                for (VdlType childTy : t.getTypes()) {
                    types.add(encodeType(enc, childTy));
                }

                enc.startTypeMessage(typeId);
                enc.writeUint64(Vom2Constants.WIRE_ONE_OF_ID.getValue());
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
                enc.endMessage(WireOneOf.VDL_TYPE);
            }
                break;
            default:
                throw new RuntimeException("Unknown wiretype for kind: " + t.getKind());
        }
    }
}
