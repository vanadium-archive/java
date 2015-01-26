package io.v.core.veyron2.vom;

import com.google.common.collect.ImmutableMap;

import io.v.core.veyron2.vdl.Types;
import io.v.core.veyron2.vdl.VdlType;

import java.util.Map;

/**
 * BootstrapType provides the set of known bootstrap type ids and their
 * corresponding VDL Type.
 */
public final class BootstrapType {
    private static final Map<VdlType, TypeID> typeToId;
    private static final Map<TypeID, VdlType> idToType;

    static {
        typeToId = ImmutableMap.<VdlType, TypeID>builder()
                // Primitive types
                .put(Types.ANY, VomConstants.WIRE_ANY_ID)
                .put(Types.TYPEOBJECT, VomConstants.WIRE_TYPE_ID)
                .put(Types.BOOL, VomConstants.WIRE_BOOL_ID)
                .put(Types.STRING, VomConstants.WIRE_STRING_ID)
                .put(Types.BYTE, VomConstants.WIRE_BYTE_ID)
                .put(Types.UINT16, VomConstants.WIRE_UINT_16_ID)
                .put(Types.UINT32, VomConstants.WIRE_UINT_32_ID)
                .put(Types.UINT64, VomConstants.WIRE_UINT_64_ID)
                .put(Types.INT16, VomConstants.WIRE_INT_16_ID)
                .put(Types.INT32, VomConstants.WIRE_INT_32_ID)
                .put(Types.INT64, VomConstants.WIRE_INT_64_ID)
                .put(Types.FLOAT32, VomConstants.WIRE_FLOAT_32_ID)
                .put(Types.FLOAT64, VomConstants.WIRE_FLOAT_64_ID)
                .put(Types.COMPLEX64, VomConstants.WIRE_COMPLEX_64_ID)
                .put(Types.COMPLEX128, VomConstants.WIRE_COMPLEX_128_ID)

                // Generic types
                .put(WireNamed.VDL_TYPE, VomConstants.WIRE_NAMED_ID)
                .put(WireEnum.VDL_TYPE, VomConstants.WIRE_ENUM_ID)
                .put(WireArray.VDL_TYPE, VomConstants.WIRE_ARRAY_ID)
                .put(WireList.VDL_TYPE, VomConstants.WIRE_LIST_ID)
                .put(WireSet.VDL_TYPE, VomConstants.WIRE_SET_ID)
                .put(WireMap.VDL_TYPE, VomConstants.WIRE_MAP_ID)
                .put(WireStruct.VDL_TYPE, VomConstants.WIRE_STRUCT_ID)
                .put(WireField.VDL_TYPE, VomConstants.WIRE_FIELD_ID)
                .put(Types.listOf(WireField.VDL_TYPE), VomConstants.WIRE_FIELD_LIST_ID)
                .put(WireUnion.VDL_TYPE, VomConstants.WIRE_UNION_ID)
                .put(WireOptional.VDL_TYPE, VomConstants.WIRE_OPTIONAL_ID)
                .put(Types.listOf(Types.BYTE), VomConstants.WIRE_BYTE_LIST_ID)
                .put(Types.listOf(Types.STRING), VomConstants.WIRE_STRING_LIST_ID)
                .put(Types.listOf(TypeID.VDL_TYPE), VomConstants.WIRE_TYPE_LIST_ID)
                .build();

        ImmutableMap.Builder<TypeID, VdlType> idToTypeBuilder =
                ImmutableMap.<TypeID, VdlType>builder();
        for (Map.Entry<VdlType, TypeID> typeToIdEntry : typeToId.entrySet()) {
            idToTypeBuilder.put(typeToIdEntry.getValue(), typeToIdEntry.getKey());
        }
        idToType = idToTypeBuilder.build();
    }

    /**
     * Returns type corresponding to provided bootstrap type id
     *
     * @param typeId the typeId whose type is to be returned
     * @return a {@code VdlType} object or null if provided type id has no associated bootstrap type
     */
    public static VdlType getBootstrapType(TypeID typeId) {
        return idToType.get(typeId);
    }

    /**
     * Returns type id corresponding to provided bootstrap type.
     *
     * @param type the type whose type id is to be returned
     * @return a {@code TypeID} object or null if provided type is not a bootstrap type
     */
    public static TypeID getBootstrapTypeId(VdlType type) {
        return typeToId.get(type);
    }
}
