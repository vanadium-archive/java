// TODO(bprosnitz) Either finish this or remove it before the 0.1 release.

package com.veyron2.vdl;

/**
 * Types provides helpers to create simple (non-recursive) types.
 */
public final class Types {
    private static Type createPrimitiveType(Kind kind) {
        return new Type(kind);
    }

    public static Type ANY = createPrimitiveType(Kind.ANY);
    public static Type BOOL = createPrimitiveType(Kind.BOOL);
    public static Type BYTE = createPrimitiveType(Kind.BYTE);
    public static Type UINT16 = createPrimitiveType(Kind.UINT16);
    public static Type UINT32 = createPrimitiveType(Kind.UINT32);
    public static Type UINT64 = createPrimitiveType(Kind.UINT64);
    public static Type INT16 = createPrimitiveType(Kind.INT16);
    public static Type INT32 = createPrimitiveType(Kind.INT32);
    public static Type INT64 = createPrimitiveType(Kind.INT64);
    public static Type FLOAT32 = createPrimitiveType(Kind.FLOAT32);
    public static Type FLOAT64 = createPrimitiveType(Kind.FLOAT64);
    public static Type COMPLEX64 = createPrimitiveType(Kind.COMPLEX64);
    public static Type COMPLEX128 = createPrimitiveType(Kind.COMPLEX128);
    public static Type STRING = createPrimitiveType(Kind.STRING);
    public static Type TYPEVAL = createPrimitiveType(Kind.TYPEVAL);

    public static Type PrimitiveTypeFromKind(Kind kind) {
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

    public static Type NilableOf(Type elem) {
        Type t = new Type(Kind.NILABLE);
        t.setElem(elem);
        return t;
    }

    public static Type EnumOf(String... labels) {
        Type t = new Type(Kind.ENUM);
        t.setLabels(labels);
        return t;
    }

    public static Type ArrayOf(int len, Type elem) {
        Type t = new Type(Kind.ARRAY);
        t.setLength(len);
        t.setElem(elem);
        return t;
    }

    public static Type ListOf(Type elem) {
        Type t = new Type(Kind.LIST);
        t.setElem(elem);
        return t;
    }

    public static Type SetOf(Type key) {
        Type t = new Type(Kind.SET);
        t.setKey(key);
        return t;
    }

    public static Type MapOf(Type key, Type elem) {
        Type t = new Type(Kind.MAP);
        t.setKey(key);
        t.setElem(elem);
        return t;
    }

    public static Type StructOf(StructField... fields) {
        Type t = new Type(Kind.STRUCT);
        t.setFields(fields);
        return t;
    }

    public static Type OneOfOf(Type... types) {
        Type t = new Type(Kind.ONE_OF);
        t.setTypes(types);
        return t;
    }

    public static Type Named(String name, Type base) {
        Type named = base.shallowCopy();
        named.setName(name);
        return named;
    }
}
