package io.veyron.veyron.veyron2.vdl;

import com.google.common.reflect.TypeToken;
import com.google.gson.annotations.SerializedName;

import io.veyron.veyron.veyron2.vdl.VdlType.PendingType;
import io.veyron.veyron.veyron2.vdl.VdlType.Builder;

import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Types provides helpers to create VDL types.
 */
public final class Types {
    /**
     * The {@code VdlType} object representing the VDL type any, it is unnamed.
     */
    public static final VdlType ANY = createPrimitiveType(Kind.ANY);

    /**
     * The {@code VdlType} object representing the VDL type bool, it is unnamed.
     */
    public static final VdlType BOOL = createPrimitiveType(Kind.BOOL);

    /**
     * The {@code VdlType} object representing the VDL type byte, it is unnamed.
     */
    public static final VdlType BYTE = createPrimitiveType(Kind.BYTE);

    /**
     * The {@code VdlType} object representing the VDL type uint16, it is unnamed.
     */
    public static final VdlType UINT16 = createPrimitiveType(Kind.UINT16);

    /**
     * The {@code VdlType} object representing the VDL type uint32, it is unnamed.
     */
    public static final VdlType UINT32 = createPrimitiveType(Kind.UINT32);

    /**
     * The {@code VdlType} object representing the VDL type uint64, it is unnamed.
     */
    public static final VdlType UINT64 = createPrimitiveType(Kind.UINT64);

    /**
     * The {@code VdlType} object representing the VDL type int16, it is unnamed.
     */
    public static final VdlType INT16 = createPrimitiveType(Kind.INT16);

    /**
     * The {@code VdlType} object representing the VDL type int32, it is unnamed.
     */
    public static final VdlType INT32 = createPrimitiveType(Kind.INT32);

    /**
     * The {@code VdlType} object representing the VDL type int64, it is unnamed.
     */
    public static final VdlType INT64 = createPrimitiveType(Kind.INT64);

    /**
     * The {@code VdlType} object representing the VDL type float32, it is unnamed.
     */
    public static final VdlType FLOAT32 = createPrimitiveType(Kind.FLOAT32);

    /**
     * The {@code VdlType} object representing the VDL type float64, it is unnamed.
     */
    public static final VdlType FLOAT64 = createPrimitiveType(Kind.FLOAT64);

    /**
     * The {@code VdlType} object representing the VDL type complex64, it is unnamed.
     */
    public static final VdlType COMPLEX64 = createPrimitiveType(Kind.COMPLEX64);

    /**
     * The {@code VdlType} object representing the VDL type complex128, it is unnamed.
     */
    public static final VdlType COMPLEX128 = createPrimitiveType(Kind.COMPLEX128);

    /**
     * The {@code VdlType} object representing the VDL type string, it is unnamed.
     */
    public static final VdlType STRING = createPrimitiveType(Kind.STRING);

    /**
     * The {@code VdlType} object representing the VDL type typeObject, it is unnamed.
     */
    public static final VdlType TYPEOBJECT = createPrimitiveType(Kind.TYPEOBJECT);

    private static final Map<Type, VdlType> typeCache = new ConcurrentHashMap<Type, VdlType>();

    static {
        typeCache.put(VdlAny.class, ANY);
        typeCache.put(VdlBool.class, BOOL);
        typeCache.put(VdlByte.class, BYTE);
        typeCache.put(VdlUint16.class, UINT16);
        typeCache.put(VdlUint32.class, UINT32);
        typeCache.put(VdlUint64.class, UINT64);
        typeCache.put(VdlInt16.class, INT16);
        typeCache.put(VdlInt32.class, INT32);
        typeCache.put(VdlInt64.class, INT64);
        typeCache.put(VdlFloat32.class, FLOAT32);
        typeCache.put(VdlFloat64.class, FLOAT64);
        typeCache.put(VdlComplex64.class, COMPLEX64);
        typeCache.put(VdlComplex128.class, COMPLEX128);
        typeCache.put(VdlString.class, STRING);
        typeCache.put(VdlTypeObject.class, TYPEOBJECT);

        typeCache.put(Boolean.TYPE, BOOL);
        typeCache.put(Boolean.class, BOOL);
        typeCache.put(Byte.TYPE, BYTE);
        typeCache.put(Byte.class, BYTE);
        typeCache.put(Short.TYPE, INT16);
        typeCache.put(Short.class, INT16);
        typeCache.put(Integer.TYPE, INT32);
        typeCache.put(Integer.class, INT32);
        typeCache.put(Long.TYPE, INT64);
        typeCache.put(Long.class, INT64);
        typeCache.put(Float.TYPE, FLOAT32);
        typeCache.put(Float.class, FLOAT32);
        typeCache.put(Double.TYPE, FLOAT64);
        typeCache.put(Double.class, FLOAT64);
        typeCache.put(String.class, STRING);
    }

    private static VdlType createPrimitiveType(Kind kind) {
        Builder builder = new Builder();
        PendingType pending = builder.newPending(kind);
        builder.build();
        return pending.built();
    }

    /**
     * Returns a {@code VdlType} object representing a VDL type of specified kind.
     */
    public static VdlType primitiveTypeFromKind(Kind kind) {
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
            case TYPEOBJECT:
                return TYPEOBJECT;
            default:
                throw new RuntimeException("Unknown primitive kind " + kind);
        }
    }

    /**
     * A helper used to create a single VDL enum type.
     */
    public static VdlType enumOf(String... labels) {
        Builder builder = new Builder();
        PendingType pending = builder.newPending(Kind.ENUM);
        for (String label : labels) {
            pending.addLabel(label);
        }
        builder.build();
        return pending.built();
    }

    /**
     * A helper used to create a single VDL fixed length array type.
     */
    public static VdlType arrayOf(int len, VdlType elem) {
        Builder builder = new Builder();
        PendingType pending = builder.newPending(Kind.ARRAY).setLength(len).setElem(elem);
        builder.build();
        return pending.built();
    }

    /**
     * A helper used to create a single VDL list type.
     */
    public static VdlType listOf(VdlType elem) {
        Builder builder = new Builder();
        PendingType pending = builder.newPending(Kind.LIST).setElem(elem);
        builder.build();
        return pending.built();
    }

    /**
     * A helper used to create a single VDL set type.
     */
    public static VdlType setOf(VdlType key) {
        Builder builder = new Builder();
        PendingType pending = builder.newPending(Kind.SET).setKey(key);
        builder.build();
        return pending.built();
    }

    /**
     * A helper used to create a single VDL map type.
     */
    public static VdlType mapOf(VdlType key, VdlType elem) {
        Builder builder = new Builder();
        PendingType pending = builder.newPending(Kind.MAP).setKey(key).setElem(elem);
        builder.build();
        return pending.built();
    }

    /**
     * A helper used to create a single VDL struct type.
     */
    public static VdlType structOf(VdlStructField... fields) {
        Builder builder = new Builder();
        PendingType pending = builder.newPending(Kind.STRUCT);
        for (VdlStructField field : fields) {
            pending.addField(field.getName(), field.getType());
        }
        builder.build();
        return pending.built();
    }

    /**
     * A helper used to create a single VDL oneOf type.
     */
    public static VdlType oneOfOf(VdlType... types) {
        Builder builder = new Builder();
        PendingType pending = builder.newPending(Kind.ONE_OF);
        for (VdlType type : types) {
            pending.addType(type);
        }
        builder.build();
        return pending.built();
    }

    /**
     * A helper used to create a single named VDL type based on another VDL type.
     */
    public static VdlType named(String name, VdlType base) {
        Builder builder = new Builder();
        PendingType pending = builder.newPending().assignBase(base).setName(name);
        builder.build();
        return pending.built();
    }

    /**
     * Creates a {@code VdlType} object corresponding a {@code java.lang.reflect.Type} object.
     * Resolves maps, sets, lists, arrays, primitives and classes generated from *.vdl files.
     * All results are statically cached.
     */
    public static VdlType getVdlTypeFromReflection(Type type) {
        if (typeCache.containsKey(type)) {
            return typeCache.get(type);
        }
        return synchronizedLookupOrBuildType(type);
    }

    private static synchronized VdlType synchronizedLookupOrBuildType(Type type) {
        if (typeCache.containsKey(type)) {
            return typeCache.get(type);
        }
        ReflectToVdlTypeBuilder builder = new ReflectToVdlTypeBuilder();
        PendingType pendingType = builder.lookupOrBuildPending(type);
        builder.buildAndCache();
        return pendingType.built();
    }

    /**
     * Builds VdlType from {@code java.lang.reflect.Type}. All results are cached in typeCahce.
     */
    private static final class ReflectToVdlTypeBuilder {
        private final Builder builder;
        private final Map<Type, PendingType> pendingTypes;

        public ReflectToVdlTypeBuilder() {
            builder = new Builder();
            pendingTypes = new HashMap<Type, PendingType>();
        }

        public void buildAndCache() {
            builder.build();
            for (Map.Entry<Type, PendingType> entry : pendingTypes.entrySet()) {
                typeCache.put(entry.getKey(), entry.getValue().built());
            }
        }

        public PendingType lookupOrBuildPending(Type type) {
            PendingType vdlType = lookupType(type);
            if (vdlType != null) {
                return vdlType;
            }
            return buildPendingFromType(type);
        }

        private PendingType lookupType(Type type) {
            if (typeCache.containsKey(type)) {
                return builder.builtPendingFromType(typeCache.get(type));
            }
            if (pendingTypes.containsKey(type)) {
                return pendingTypes.get(type);
            }
            return null;
        }

        private PendingType buildPendingFromType(Type type) {
            Class<?> klass;
            Type[] elementTypes;
            if (type instanceof Class) {
                klass = (Class<?>) type;
                return buildPendingFromClass(klass);
            } else if (type instanceof ParameterizedType) {
                klass = (Class<?>) ((ParameterizedType) type).getRawType();
                elementTypes = ((ParameterizedType) type).getActualTypeArguments();
            } else if (type instanceof GenericArrayType) {
                klass = List.class;
                elementTypes = new Type[1];
                elementTypes[0] = (((GenericArrayType) type).getGenericComponentType());
            } else {
                throw new IllegalArgumentException("Unable to create VDL Type for type " + type);
            }

            PendingType pending;
            if (List.class.isAssignableFrom(klass)) {
                pending = builder.listOf(lookupOrBuildPending(elementTypes[0]));
            } else if (Set.class.isAssignableFrom(klass)) {
                pending = builder.setOf(lookupOrBuildPending(elementTypes[0]));
            } else if (Map.class.isAssignableFrom(klass)) {
                pending = builder.mapOf(lookupOrBuildPending(elementTypes[0]),
                        lookupOrBuildPending(elementTypes[1]));
            } else {
                throw new IllegalArgumentException("Unable to create VDL Type for type " + type);
            }
            pendingTypes.put(type, pending);
            return pending;
        }


        private PendingType buildPendingFromClass(Class<?> klass) {
            PendingType pending;
            if (klass.isArray()) {
                pending = builder.listOf(lookupOrBuildPending(klass.getComponentType()));
                pendingTypes.put(klass, pending);
                return pending;
            }

            pending = builder.newPending();
            pendingTypes.put(klass, pending);
            Class<?> superClass = klass.getSuperclass();
            if (superClass == VdlEnum.class) {
                populateEnum(pending, klass);
            } else if (superClass == AbstractVdlStruct.class) {
                populateStruct(pending, klass);
            } else if (superClass == VdlOneOf.class) {
                populateOneOf(pending, klass);
            } else if (superClass == VdlArray.class) {
                populateArray(pending, klass);
            } else {
                pending.assignBase(lookupOrBuildPending(klass.getGenericSuperclass()));
            }
            GeneratedFromVdlType vdlName = klass.getAnnotation(GeneratedFromVdlType.class);
            if (vdlName != null) {
                pending.setName(vdlName.value());
            } else {
                pending.setName(klass.getName());
            }
            return pending;
        }

        private void populateEnum(PendingType pending, Class<?> klass) {
            pending.setKind(Kind.ENUM);
            for (Field field : klass.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())
                        && field.getType() == klass) {
                    pending.addLabel(field.getName());
                }
            }
        }

        private void populateStruct(PendingType pending, Class<?> klass) {
            pending.setKind(Kind.STRUCT);
            for (Field field : klass.getDeclaredFields()) {
                SerializedName name = field.getAnnotation(SerializedName.class);
                if (name != null) {
                    pending.addField(name.value(), lookupOrBuildPending(field.getGenericType()));
                }
            }
        }

        private void populateOneOf(PendingType pending, Class<?> klass) {
            pending.setKind(Kind.ONE_OF);
            try {
                @SuppressWarnings("unchecked")
                List<TypeToken<?>> types = (List<TypeToken<?>>) klass.getField("TYPES").get(null);
                for (TypeToken<?> typeToken : types) {
                    pending.addType(lookupOrBuildPending(typeToken.getType()));
                }
            } catch (Exception e) {
                throw new IllegalArgumentException("Unable to create VDL Type for type " + klass);
            }
        }

        private void populateArray(PendingType pending, Class<?> klass) {
            pending.setKind(Kind.ARRAY);
            Type elementType = ((ParameterizedType) klass.getGenericSuperclass())
                    .getActualTypeArguments()[0];
            pending.setElem(lookupOrBuildPending(elementType));
            try {
                pending.setLength(klass.getField("LENGTH").getInt(null));
            } catch (Exception e) {
                throw new IllegalArgumentException("Unable to create VDL Type for type " + klass);
            }
        }
    }
}
