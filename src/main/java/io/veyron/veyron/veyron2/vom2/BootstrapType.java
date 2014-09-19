// TODO(bprosnitz) Either finish this or remove it before the 0.1 release.

package io.veyron.veyron.veyron2.vom2;

import java.util.HashMap;
import java.util.Map;

import io.veyron.veyron.veyron2.vdl.Kind;
import io.veyron.veyron.veyron2.vdl.StructField;
import io.veyron.veyron.veyron2.vdl.Type;
import io.veyron.veyron.veyron2.vdl.Types;

/**
 * BootstrapType provides the set of known bootstrap type ids and their
 * corresponding VDL Type.
 */
enum BootstrapType {
    ANY(1, Types.ANY),
    TYPEID(2, Types.Named("TypeID", Types.UINT64)),
    BOOL(3, Types.BOOL),
    STRING(4, Types.STRING),
    BYTE(5, Types.BYTE),
    UINT16(6, Types.UINT16),
    UINT32(7, Types.UINT32),
    UINT64(8, Types.UINT64),
    INT16(9, Types.INT16),
    INT32(10, Types.INT32),
    INT64(11, Types.INT64),
    FLOAT32(12, Types.FLOAT32),
    FLOAT64(13, Types.FLOAT64),
    COMPLEX64(14, Types.COMPLEX64),
    COMPLEX128(15, Types.COMPLEX128),

    WIRE_NAMED(16, getNamedBootstrapType()),
    WIRE_ENUM(17, getEnumBootstrapType()),
    WIRE_ARRAY(18, getArrayBootstrapType()),
    WIRE_LIST(19, getListBootstrapType()),
    WIRE_SET(20, getSetBootstrapType()),
    WIRE_MAP(21, getMapBootstrapType()),
    WIRE_STRUCT(22, getStructBootstrapType()),
    WIRE_FIELD(23, getFieldBootstrapType()),
    WIRE_FIELD_LIST(24, Types.ListOf(getFieldBootstrapType())),
    WIRE_ONE_OF(25, getOneOfBootstrapType()),

    LIST_BYTE(26, Types.ListOf(Types.BYTE)),
    LIST_STRING(27, Types.ListOf(Types.STRING)),
    LIST_TYPEID(28, Types.ListOf(Types.Named("TypeID", Types.UINT64)));

    private final int id;
    private final Type type;

    private BootstrapType(final int id, final Type t) {
        this.id = id;
        this.type = t;
    }

    public int getId() {
        return id;
    }

    public Type getType() {
        return type;
    }

    private static final Map<Type, BootstrapType> bootstrapTypeMap = new HashMap<Type, BootstrapType>();
    private static final Map<Long, BootstrapType> bootstrapTypeIdMap = new HashMap<Long, BootstrapType>();
    static {
        for (BootstrapType bt : BootstrapType.values()) {
            bootstrapTypeMap.put(bt.getType(), bt);
            bootstrapTypeIdMap.put((long) bt.getId(), bt);
        }
    }

    public static BootstrapType findBootstrapType(Type t) {
        return bootstrapTypeMap.get(t);
    }

    public static BootstrapType findBootstrapTypeById(long id) {
        return bootstrapTypeIdMap.get(id);
    }

    private static Type getNamedBootstrapType() {
        Type type = new Type(Kind.STRUCT);
        StructField[] fields = new StructField[2];
        fields[0] = new StructField("name", Types.STRING);
        fields[1] = new StructField("base", Types.Named("TypeID", Types.UINT64));
        type.setFields(fields);
        return type;
    }

    private static Type getEnumBootstrapType() {
        Type type = new Type(Kind.STRUCT);
        StructField[] fields = new StructField[2];
        fields[0] = new StructField("name", Types.STRING);
        fields[1] = new StructField("labels", Types.ListOf(Types.STRING));
        type.setFields(fields);
        return type;
    }

    private static Type getArrayBootstrapType() {
        Type type = new Type(Kind.STRUCT);
        StructField[] fields = new StructField[3];
        fields[0] = new StructField("name", Types.STRING);
        fields[1] = new StructField("elem", Types.Named("TypeID", Types.UINT64));
        fields[2] = new StructField("len", Types.UINT64);
        type.setFields(fields);
        return type;
    }

    private static Type getListBootstrapType() {
        Type type = new Type(Kind.STRUCT);
        StructField[] fields = new StructField[2];
        fields[0] = new StructField("name", Types.STRING);
        fields[1] = new StructField("elem", Types.Named("TypeID", Types.UINT64));
        type.setFields(fields);
        return type;
    }

    private static Type getSetBootstrapType() {
        Type type = new Type(Kind.STRUCT);
        StructField[] fields = new StructField[2];
        fields[0] = new StructField("name", Types.STRING);
        fields[1] = new StructField("elem", Types.Named("TypeID", Types.UINT64));
        type.setFields(fields);
        return type;
    }

    private static Type getMapBootstrapType() {
        Type type = new Type(Kind.STRUCT);
        StructField[] fields = new StructField[3];
        fields[0] = new StructField("name", Types.STRING);
        fields[1] = new StructField("key", Types.Named("TypeID", Types.UINT64));
        fields[2] = new StructField("elem", Types.Named("TypeID", Types.UINT64));
        type.setFields(fields);
        return type;
    }

    private static Type getFieldBootstrapType() {
        Type type = new Type(Kind.STRUCT);
        StructField[] fields = new StructField[2];
        fields[0] = new StructField("name", Types.STRING);
        fields[1] = new StructField("type", Types.Named("TypeID", Types.UINT64));
        type.setFields(fields);
        return type;
    }

    private static Type getStructBootstrapType() {
        Type type = new Type(Kind.STRUCT);
        StructField[] fields = new StructField[2];
        fields[0] = new StructField("name", Types.STRING);
        fields[1] = new StructField("fields", Types.ListOf(getFieldBootstrapType()));
        type.setFields(fields);
        return type;
    }

    public static Type getOneOfBootstrapType() {
        Type type = new Type(Kind.STRUCT);
        StructField[] fields = new StructField[2];
        fields[0] = new StructField("name", Types.STRING);
        fields[1] = new StructField("types", Types.ListOf(Types.Named("TypeID", Types.UINT64)));
        type.setFields(fields);
        return type;
    }
}
