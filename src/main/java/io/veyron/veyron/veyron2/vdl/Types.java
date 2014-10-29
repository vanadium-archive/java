// TODO(bprosnitz) Either finish this or remove it before the 0.1 release.

package io.veyron.veyron.veyron2.vdl;

/**
 * Types provides helpers to create simple (non-recursive) types.
 */
public final class Types {
    private static VdlType createPrimitiveType(Kind kind) {
        return new VdlType(kind);
    }

    public static VdlType ANY = createPrimitiveType(Kind.ANY);
    public static VdlType BOOL = createPrimitiveType(Kind.BOOL);
    public static VdlType BYTE = createPrimitiveType(Kind.BYTE);
    public static VdlType UINT16 = createPrimitiveType(Kind.UINT16);
    public static VdlType UINT32 = createPrimitiveType(Kind.UINT32);
    public static VdlType UINT64 = createPrimitiveType(Kind.UINT64);
    public static VdlType INT16 = createPrimitiveType(Kind.INT16);
    public static VdlType INT32 = createPrimitiveType(Kind.INT32);
    public static VdlType INT64 = createPrimitiveType(Kind.INT64);
    public static VdlType FLOAT32 = createPrimitiveType(Kind.FLOAT32);
    public static VdlType FLOAT64 = createPrimitiveType(Kind.FLOAT64);
    public static VdlType COMPLEX64 = createPrimitiveType(Kind.COMPLEX64);
    public static VdlType COMPLEX128 = createPrimitiveType(Kind.COMPLEX128);
    public static VdlType STRING = createPrimitiveType(Kind.STRING);
    public static VdlType TYPEVAL = createPrimitiveType(Kind.TYPEVAL);

    public static VdlType PrimitiveTypeFromKind(Kind kind) {
        switch (kind) {
            case ANY:
                return ANY;
            case BOOL:
                return BOOL;
            case BYTE:
                return BYTE;
            case UINT16:
                return UINT16;
            case UINT32:
                return UINT32;
            case UINT64:
                return UINT64;
            case INT16:
                return INT16;
            case INT32:
                return INT32;
            case INT64:
                return INT64;
            case FLOAT32:
                return FLOAT32;
            case FLOAT64:
                return FLOAT64;
            case COMPLEX64:
                return COMPLEX64;
            case COMPLEX128:
                return COMPLEX128;
            case STRING:
                return STRING;
            case TYPEVAL:
                return TYPEVAL;
            default:
                throw new RuntimeException("Unknown primitive kind " + kind);
        }
    }

    public static VdlType NilableOf(VdlType elem) {
        VdlType t = new VdlType(Kind.NILABLE);
        t.setElem(elem);
        return t;
    }

    public static VdlType EnumOf(String... labels) {
        VdlType t = new VdlType(Kind.ENUM);
        t.setLabels(labels);
        return t;
    }

    public static VdlType ArrayOf(int len, VdlType elem) {
        VdlType t = new VdlType(Kind.ARRAY);
        t.setLength(len);
        t.setElem(elem);
        return t;
    }

    public static VdlType ListOf(VdlType elem) {
        VdlType t = new VdlType(Kind.LIST);
        t.setElem(elem);
        return t;
    }

    public static VdlType SetOf(VdlType key) {
        VdlType t = new VdlType(Kind.SET);
        t.setKey(key);
        return t;
    }

    public static VdlType MapOf(VdlType key, VdlType elem) {
        VdlType t = new VdlType(Kind.MAP);
        t.setKey(key);
        t.setElem(elem);
        return t;
    }

    public static VdlType StructOf(StructField... fields) {
        VdlType t = new VdlType(Kind.STRUCT);
        t.setFields(fields);
        return t;
    }

    public static VdlType OneOfOf(VdlType... types) {
        VdlType t = new VdlType(Kind.ONE_OF);
        t.setTypes(types);
        return t;
    }

    public static VdlType Named(String name, VdlType base) {
        VdlType named = base.shallowCopy();
        named.setName(name);
        return named;
    }
}
