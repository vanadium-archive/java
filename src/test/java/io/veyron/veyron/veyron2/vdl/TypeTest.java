package io.veyron.veyron.veyron2.vdl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.reflect.TypeToken;
import com.google.gson.annotations.SerializedName;

import junit.framework.TestCase;

import io.veyron.veyron.veyron2.vdl.VdlType.Builder;
import io.veyron.veyron.veyron2.vdl.VdlType.PendingType;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Tests methods on VDL Types.
 */
public class TypeTest extends TestCase {
    private static final class MyBool extends VdlBool {}
    private static final class MyByte extends VdlByte {}
    private static final class MyUint16 extends VdlUint16 {}
    private static final class MyUint32 extends VdlUint32 {}
    private static final class MyUint64 extends VdlUint64 {}
    private static final class MyInt16 extends VdlInt16 {}
    private static final class MyInt32 extends VdlInt32 {}
    private static final class MyInt64 extends VdlInt64 {}
    private static final class MyFloat32 extends VdlFloat32 {}
    private static final class MyFloat64 extends VdlFloat64 {}
    private static final class MyString extends VdlString {}

    private static final class MyComplex64 extends VdlComplex64 {
        public MyComplex64() {
            super(0);
        }
    }

    private static final class MyComplex128 extends VdlComplex128 {
        public MyComplex128() {
            super(0);
        }
    }

    private static final class MyOneOf extends VdlOneOf {
        @SuppressWarnings("unused")
        public static final ImmutableList<TypeToken<?>> TYPES = ImmutableList.<TypeToken<?>>of(
                new TypeToken<MyInt16>() {}, new TypeToken<VdlInt32>() {},
                new TypeToken<Long>() {});

        public MyOneOf() {
            super(Types.getVdlTypeFromReflection(MyOneOf.class));
        }
    }

    private static final class MyEnum extends VdlEnum {
        @SuppressWarnings("unused")
        public static final MyEnum LABEL1 = new MyEnum("LABEL1");
        @SuppressWarnings("unused")
        public static final MyEnum LABEL2 = new MyEnum("LABEL2");
        @SuppressWarnings("unused")
        public static final MyEnum LABEL3 = new MyEnum("LABEL3");

        private MyEnum(String name) {
            super(Types.getVdlTypeFromReflection(MyEnum.class), name);
        }
    }

    private static final class MyArray12 extends VdlArray<Set<MyOneOf>> {
        @SuppressWarnings("unused")
        public static final int LENGTH = 12;

        public MyArray12(Set<MyOneOf>[] value) {
            super(Types.getVdlTypeFromReflection(MyArray12.class), value);
        }
    }

    private static final class MyList extends VdlList<List<MyArray12>> {
        public MyList(List<List<MyArray12>> impl) {
            super(Types.getVdlTypeFromReflection(MyList.class), impl);
        }
    }

    private static final class MySet extends VdlSet<Set<MyList>> {
        public MySet(Set<Set<MyList>> impl) {
            super(Types.getVdlTypeFromReflection(MySet.class), impl);
        }
    }

    private static final class MyMap extends VdlMap<MyEnum, Map<MySet, MySet>> {
        public MyMap(Map<MyEnum, Map<MySet, MySet>> impl) {
            super(Types.getVdlTypeFromReflection(MyMap.class), impl);
        }
    }

    private static final class MyStruct extends AbstractVdlStruct {
        @SerializedName("ByteArray")
        private byte[][] byteArray;
        @SerializedName("List")
        private Set<MyMap> list;
        @SerializedName("Cycle")
        private List<MyStruct> cycle;

        public MyStruct() {
            super(Types.getVdlTypeFromReflection(MyStruct.class));
        }
    }

    private static final Map<VdlType, Class<?>> myTypes;

    static {
        VdlType myBool = Types.named(MyBool.class.getName(), Types.BOOL);
        VdlType myByte = Types.named(MyByte.class.getName(), Types.BYTE);
        VdlType myUint16 = Types.named(MyUint16.class.getName(), Types.UINT16);
        VdlType myUint32 = Types.named(MyUint32.class.getName(), Types.UINT32);
        VdlType myUint64 = Types.named(MyUint64.class.getName(), Types.UINT64);
        VdlType myInt16 = Types.named(MyInt16.class.getName(), Types.INT16);
        VdlType myInt32 = Types.named(MyInt32.class.getName(), Types.INT32);
        VdlType myInt64 = Types.named(MyInt64.class.getName(), Types.INT64);
        VdlType myFloat32 = Types.named(MyFloat32.class.getName(), Types.FLOAT32);
        VdlType myFloat64 = Types.named(MyFloat64.class.getName(), Types.FLOAT64);
        VdlType myString = Types.named(MyString.class.getName(), Types.STRING);
        VdlType myComplex64 = Types.named(MyComplex64.class.getName(), Types.COMPLEX64);
        VdlType myComplex128 = Types.named(MyComplex128.class.getName(), Types.COMPLEX128);

        VdlType myOneOf = Types.named(MyOneOf.class.getName(),
                Types.oneOfOf(myInt16, Types.INT32, Types.INT64));
        VdlType myEnum = Types.named(MyEnum.class.getName(),
                Types.enumOf("LABEL1", "LABEL2", "LABEL3"));
        VdlType myArray12 = Types.named(MyArray12.class.getName(),
                Types.arrayOf(12, Types.setOf(myOneOf)));
        VdlType myList = Types.named(MyList.class.getName(), Types.listOf(Types.listOf(myArray12)));
        VdlType mySet = Types.named(MySet.class.getName(), Types.setOf(Types.setOf(myList)));
        VdlType myMap = Types.named(MyMap.class.getName(),
                Types.mapOf(myEnum, Types.mapOf(mySet, mySet)));

        Builder builder = new Builder();
        PendingType pendingStruct = builder.newPending(Kind.STRUCT)
                .setName(MyStruct.class.getName());
        pendingStruct.addField("ByteArray", Types.listOf(Types.listOf(Types.BYTE)))
                .addField("List", Types.setOf(myMap))
                .addField("Cycle", builder.listOf(pendingStruct));
        builder.build();
        VdlType myStruct = pendingStruct.built();

        myTypes = new ImmutableMap.Builder<VdlType, Class<?>>()
                .put(myBool, MyBool.class)
                .put(myByte, MyByte.class)
                .put(myUint16, MyUint16.class)
                .put(myUint32, MyUint32.class)
                .put(myUint64, MyUint64.class)
                .put(myInt16, MyInt16.class)
                .put(myInt32, MyInt32.class)
                .put(myInt64, MyInt64.class)
                .put(myFloat32, MyFloat32.class)
                .put(myFloat64, MyFloat64.class)
                .put(myString, MyString.class)
                .put(myComplex64, MyComplex64.class)
                .put(myComplex128, MyComplex128.class)
                .put(myOneOf, MyOneOf.class)
                .put(myEnum, MyEnum.class)
                .put(myArray12, MyArray12.class)
                .put(myList, MyList.class)
                .put(mySet, MySet.class)
                .put(myMap, MyMap.class)
                .put(myStruct, MyStruct.class)
                .build();
    }

    public void testGetVdlTypeFromReflection() {
        for (Map.Entry<VdlType, Class<?>> entry : myTypes.entrySet()) {
            assertEquals("Type for class: " + entry.getValue(),
                    entry.getKey(), Types.getVdlTypeFromReflection(entry.getValue()));
        }
    }

    public void testEquals() {
        VdlType primitive = new VdlType(Kind.UINT32);

        VdlType list = new VdlType(Kind.LIST);
        list.setElem(primitive);

        VdlType recursiveSet = new VdlType(Kind.SET);
        recursiveSet.setKey(recursiveSet);

        VdlType recursiveList = new VdlType(Kind.LIST);
        VdlType recursiveStruct = new VdlType(Kind.STRUCT);
        recursiveList.setElem(recursiveStruct);
        VdlStructField[] fields = new VdlStructField[2];
        fields[0] = new VdlStructField("rec", recursiveSet);
        fields[1] = new VdlStructField("rec2", recursiveList);
        recursiveStruct.setFields(fields);

        VdlType[] types = new VdlType[] {
                primitive, list, recursiveSet,
                recursiveStruct, recursiveList
        };
        for (VdlType type : types) {
            for (VdlType other : types) {
                if (type == other) {
                    assertEquals(type, other);
                } else {
                    assertFalse(type.equals(other));
                }
            }

            VdlType copy = type.shallowCopy();
            assertEquals(type, copy);
        }
    }

    public void testHashCode() {
        VdlType primitive = new VdlType(Kind.UINT32);

        VdlType list = new VdlType(Kind.LIST);
        list.setElem(primitive);

        VdlType recursiveSet = new VdlType(Kind.SET);
        recursiveSet.setKey(recursiveSet);

        VdlType recursiveSetCopy = new VdlType(Kind.SET);
        recursiveSetCopy.setKey(recursiveSetCopy);

        assertEquals(recursiveSet.hashCode(), recursiveSetCopy.hashCode());
        assertFalse(list.hashCode() == recursiveSet.hashCode());
    }

    /**
     * Tests equals on equivalent types that have structural differences.
     * Here, the key and value of one map are the same type while they are different
     * but equivalent types for the other map.
     */
    public void testEqualsStructuralDifferences() {
        // Both key and elem have the same type.
        VdlType stringMap = new VdlType(Kind.MAP);
        VdlType recursiveSet = new VdlType(Kind.SET);
        recursiveSet.setKey(recursiveSet);
        stringMap.setKey(recursiveSet);
        stringMap.setElem(recursiveSet);

        // Key and elem have different but equivalent types.
        VdlType otherTypeStringMap = new VdlType(Kind.MAP);
        VdlType recursiveSetKey = new VdlType(Kind.SET);
        recursiveSetKey.setKey(recursiveSetKey);
        otherTypeStringMap.setKey(recursiveSetKey);
        VdlType recursiveSetElem = new VdlType(Kind.SET);
        recursiveSetElem.setKey(recursiveSetElem);
        otherTypeStringMap.setElem(recursiveSetElem);

        assertEquals(stringMap, otherTypeStringMap);
        assertEquals(otherTypeStringMap, stringMap);
    }
}
