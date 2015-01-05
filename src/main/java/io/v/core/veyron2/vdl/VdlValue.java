package io.v.core.veyron2.vdl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Value is the generic representation of any value expressible in veyron.  All values are typed.
 */
public abstract class VdlValue implements Serializable {
    private final VdlType type;

    protected VdlValue(VdlType type) {
        this.type = type;
    }

    protected void assertKind(Kind kind) {
        if (type.getKind() != kind) {
            throw new IllegalArgumentException("Kind of VDL type should be " + kind);
        }
    }

    /**
     * Returns the runtime VDL type of this value.
     *
     * @return The {@code Type} object that represents the runtime
     *         VDL type of this VDL value.
     */
    public VdlType vdlType() {
        return type;
    }

    /**
     * Returns the zero representation for each kind of VDL type.
     */
    public static VdlValue zeroValue(VdlType type) {
        switch (type.getKind()) {
            case ANY:
                return new VdlAny();
            case ARRAY:
                VdlValue[] backingArray = new VdlValue[type.getLength()];
                VdlValue elemValue = zeroValue(type.getElem());
                for (int i = 0; i < type.getLength(); i++) {
                    backingArray[i] = elemValue;
                }
                return new VdlArray<VdlValue>(type, backingArray);
            case BOOL:
                return new VdlBool();
            case BYTE:
                return new VdlByte();
            case COMPLEX128:
                return new VdlComplex128(0);
            case COMPLEX64:
                return new VdlComplex64(0);
            case ENUM:
                return new VdlEnum(type, type.getLabels().get(0));
            case FLOAT32:
                return new VdlFloat32();
            case FLOAT64:
                return new VdlFloat64();
            case INT16:
                return new VdlInt16();
            case INT32:
                return new VdlInt32();
            case INT64:
                return new VdlInt64();
            case LIST:
                return new VdlList<VdlValue>(type, new ArrayList<VdlValue>());
            case MAP:
                return new VdlMap<VdlValue, VdlValue>(type, new HashMap<VdlValue, VdlValue>());
            case UNION:
                VdlField zeroField = type.getFields().get(0);
                return new VdlUnion(type, 0, zeroField.getType(), zeroValue(zeroField.getType()));
            case OPTIONAL:
                return new VdlOptional<VdlValue>(type);
            case SET:
                return new VdlSet<VdlValue>(type, new HashSet<VdlValue>());
            case STRING:
                return new VdlString();
            case STRUCT:
                VdlStruct struct = new VdlStruct(type);
                for (VdlField field : type.getFields()) {
                    struct.assignField(field.getName(), zeroValue(field.getType()));
                }
                return struct;
            case TYPEOBJECT:
                return new VdlTypeObject(Types.ANY);
            case UINT16:
                return new VdlUint16();
            case UINT32:
                return new VdlUint32();
            case UINT64:
                return new VdlUint64();
            default:
                throw new IllegalArgumentException("Unhandled kind " + type.getKind());
        }
    }
}
