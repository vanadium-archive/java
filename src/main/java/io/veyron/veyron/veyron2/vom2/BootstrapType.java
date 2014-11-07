package io.veyron.veyron.veyron2.vom2;

import java.util.HashMap;
import java.util.Map;

import io.veyron.veyron.veyron2.vdl.Kind;
import io.veyron.veyron.veyron2.vdl.VdlStructField;
import io.veyron.veyron.veyron2.vdl.VdlType;
import io.veyron.veyron.veyron2.vdl.Types;

/**
 * BootstrapType provides the set of known bootstrap type ids and their
 * corresponding VDL Type.
 */
enum BootstrapType {
    ANY(1, Types.ANY),
    TYPEID(2, Types.named("TypeID", Types.UINT64)),
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
    WIRE_FIELD_LIST(24, Types.listOf(getFieldBootstrapType())),
    WIRE_ONE_OF(25, getOneOfBootstrapType()),

    LIST_BYTE(26, Types.listOf(Types.BYTE)),
    LIST_STRING(27, Types.listOf(Types.STRING)),
    LIST_TYPEID(28, Types.listOf(Types.named("TypeID", Types.UINT64)));

    private final int id;
    private final VdlType type;

    private BootstrapType(final int id, final VdlType t) {
        this.id = id;
        this.type = t;
    }

    public int getId() {
        return id;
    }

    public VdlType getType() {
        return type;
    }

    private static final Map<VdlType, BootstrapType> bootstrapTypeMap
            = new HashMap<VdlType, BootstrapType>();
    private static final Map<Long, BootstrapType> bootstrapTypeIdMap
            = new HashMap<Long, BootstrapType>();
    static {
        for (BootstrapType bt : BootstrapType.values()) {
            bootstrapTypeMap.put(bt.getType(), bt);
            bootstrapTypeIdMap.put((long) bt.getId(), bt);
        }
    }

    public static BootstrapType findBootstrapType(VdlType t) {
        return bootstrapTypeMap.get(t);
    }

    public static BootstrapType findBootstrapTypeById(long id) {
        return bootstrapTypeIdMap.get(id);
    }

    private static VdlType getNamedBootstrapType() {
        VdlType type = new VdlType(Kind.STRUCT);
        VdlStructField[] fields = new VdlStructField[2];
        fields[0] = new VdlStructField("name", Types.STRING);
        fields[1] = new VdlStructField("base", Types.named("TypeID", Types.UINT64));
        type.setFields(fields);
        return type;
    }

    private static VdlType getEnumBootstrapType() {
        VdlType type = new VdlType(Kind.STRUCT);
        VdlStructField[] fields = new VdlStructField[2];
        fields[0] = new VdlStructField("name", Types.STRING);
        fields[1] = new VdlStructField("labels", Types.listOf(Types.STRING));
        type.setFields(fields);
        return type;
    }

    private static VdlType getArrayBootstrapType() {
        VdlType type = new VdlType(Kind.STRUCT);
        VdlStructField[] fields = new VdlStructField[3];
        fields[0] = new VdlStructField("name", Types.STRING);
        fields[1] = new VdlStructField("elem", Types.named("TypeID", Types.UINT64));
        fields[2] = new VdlStructField("len", Types.UINT64);
        type.setFields(fields);
        return type;
    }

    private static VdlType getListBootstrapType() {
        VdlType type = new VdlType(Kind.STRUCT);
        VdlStructField[] fields = new VdlStructField[2];
        fields[0] = new VdlStructField("name", Types.STRING);
        fields[1] = new VdlStructField("elem", Types.named("TypeID", Types.UINT64));
        type.setFields(fields);
        return type;
    }

    private static VdlType getSetBootstrapType() {
        VdlType type = new VdlType(Kind.STRUCT);
        VdlStructField[] fields = new VdlStructField[2];
        fields[0] = new VdlStructField("name", Types.STRING);
        fields[1] = new VdlStructField("elem", Types.named("TypeID", Types.UINT64));
        type.setFields(fields);
        return type;
    }

    private static VdlType getMapBootstrapType() {
        VdlType type = new VdlType(Kind.STRUCT);
        VdlStructField[] fields = new VdlStructField[3];
        fields[0] = new VdlStructField("name", Types.STRING);
        fields[1] = new VdlStructField("key", Types.named("TypeID", Types.UINT64));
        fields[2] = new VdlStructField("elem", Types.named("TypeID", Types.UINT64));
        type.setFields(fields);
        return type;
    }

    private static VdlType getFieldBootstrapType() {
        VdlType type = new VdlType(Kind.STRUCT);
        VdlStructField[] fields = new VdlStructField[2];
        fields[0] = new VdlStructField("name", Types.STRING);
        fields[1] = new VdlStructField("type", Types.named("TypeID", Types.UINT64));
        type.setFields(fields);
        return type;
    }

    private static VdlType getStructBootstrapType() {
        VdlType type = new VdlType(Kind.STRUCT);
        VdlStructField[] fields = new VdlStructField[2];
        fields[0] = new VdlStructField("name", Types.STRING);
        fields[1] = new VdlStructField("fields", Types.listOf(getFieldBootstrapType()));
        type.setFields(fields);
        return type;
    }

    public static VdlType getOneOfBootstrapType() {
        VdlType type = new VdlType(Kind.STRUCT);
        VdlStructField[] fields = new VdlStructField[2];
        fields[0] = new VdlStructField("name", Types.STRING);
        fields[1] = new VdlStructField("types", Types.listOf(Types.named("TypeID", Types.UINT64)));
        type.setFields(fields);
        return type;
    }
}
