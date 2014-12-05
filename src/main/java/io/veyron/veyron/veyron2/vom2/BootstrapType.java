package io.veyron.veyron.veyron2.vom2;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

import io.veyron.veyron.veyron2.vdl.VdlType;
import io.veyron.veyron.veyron2.vdl.Types;

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
                .put(Types.ANY, Vom2Constants.WIRE_ANY_ID)
                .put(Types.TYPEOBJECT, Vom2Constants.WIRE_TYPE_ID)
                .put(Types.BOOL, Vom2Constants.WIRE_BOOL_ID)
                .put(Types.STRING, Vom2Constants.WIRE_STRING_ID)
                .put(Types.BYTE, Vom2Constants.WIRE_BYTE_ID)
                .put(Types.UINT16, Vom2Constants.WIRE_UINT_16_ID)
                .put(Types.UINT32, Vom2Constants.WIRE_UINT_32_ID)
                .put(Types.UINT64, Vom2Constants.WIRE_UINT_64_ID)
                .put(Types.INT16, Vom2Constants.WIRE_INT_16_ID)
                .put(Types.INT32, Vom2Constants.WIRE_INT_32_ID)
                .put(Types.INT64, Vom2Constants.WIRE_INT_64_ID)
                .put(Types.FLOAT32, Vom2Constants.WIRE_FLOAT_32_ID)
                .put(Types.FLOAT64, Vom2Constants.WIRE_FLOAT_64_ID)
                .put(Types.COMPLEX64, Vom2Constants.WIRE_COMPLEX_64_ID)
                .put(Types.COMPLEX128, Vom2Constants.WIRE_COMPLEX_128_ID)

                // Generic types
                .put(WireNamed.VDL_TYPE, Vom2Constants.WIRE_NAMED_ID)
                .put(WireEnum.VDL_TYPE, Vom2Constants.WIRE_ENUM_ID)
                .put(WireArray.VDL_TYPE, Vom2Constants.WIRE_ARRAY_ID)
                .put(WireList.VDL_TYPE, Vom2Constants.WIRE_LIST_ID)
                .put(WireSet.VDL_TYPE, Vom2Constants.WIRE_SET_ID)
                .put(WireMap.VDL_TYPE, Vom2Constants.WIRE_MAP_ID)
                .put(WireStruct.VDL_TYPE, Vom2Constants.WIRE_STRUCT_ID)
                .put(WireField.VDL_TYPE, Vom2Constants.WIRE_FIELD_ID)
                .put(Types.listOf(WireField.VDL_TYPE), Vom2Constants.WIRE_FIELD_LIST_ID)
                .put(WireOneOf.VDL_TYPE, Vom2Constants.WIRE_ONE_OF_ID)
                .put(WireOptional.VDL_TYPE, Vom2Constants.WIRE_OPTIONAL_ID)
                .put(Types.listOf(Types.BYTE), Vom2Constants.WIRE_BYTE_LIST_ID)
                .put(Types.listOf(Types.STRING), Vom2Constants.WIRE_STRING_LIST_ID)
                .put(Types.listOf(TypeID.VDL_TYPE), Vom2Constants.WIRE_TYPE_LIST_ID)
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
