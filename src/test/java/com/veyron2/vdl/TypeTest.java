// TODO(bprosnitz) Either finish this or remove it before the 0.1 release.

package com.veyron2.vdl;

import junit.framework.TestCase;

/**
 * Tests methods on VDL Types.
 */
public class TypeTest extends TestCase {
    public void testEquals() {
        Type primitive = new Type(Kind.UINT32);

        Type list = new Type(Kind.LIST);
        list.setElem(primitive);

        Type recursiveSet = new Type(Kind.SET);
        recursiveSet.setKey(recursiveSet);

        Type recursiveStruct = new Type(Kind.STRUCT);
        StructField[] fields = new StructField[2];
        recursiveStruct.setFields(fields);
        fields[0] = new StructField("rec", recursiveSet);
        Type recursiveList = new Type(Kind.LIST);
        recursiveList.setElem(recursiveStruct);
        fields[1] = new StructField("rec2", recursiveList);

        Type[] types = new Type[] {
                primitive, list, recursiveSet,
                recursiveStruct, recursiveList
        };
        for (Type type : types) {
            for (Type other : types) {
                if (type == other) {
                    assertEquals(type, other);
                } else {
                    assertFalse(type.equals(other));
                }
            }

            Type copy = new Type(type.getKind());
            copy.setName(type.getName());
            copy.setLabels(type.getLabels());
            copy.setLength(type.getLength());
            copy.setKey(type.getKey());
            copy.setElem(type.getElem());
            copy.setFields(type.getFields());

            assertEquals(type, copy);
        }
    }

    public void testHashCode() {
        Type primitive = new Type(Kind.UINT32);

        Type list = new Type(Kind.LIST);
        list.setElem(primitive);

        Type recursiveSet = new Type(Kind.SET);
        recursiveSet.setKey(recursiveSet);

        Type recursiveSetCopy = new Type(Kind.SET);
        recursiveSetCopy.setKey(recursiveSetCopy);

        assertEquals(recursiveSet.hashCode(), recursiveSetCopy.hashCode());
        assertFalse(list.hashCode() == recursiveSet.hashCode());
    }

    public void testDeepCopy() {
        Type recursiveSet = new Type(Kind.SET);
        recursiveSet.setKey(recursiveSet);

        Type recursiveStruct = new Type(Kind.STRUCT);
        StructField[] fields = new StructField[2];
        recursiveStruct.setFields(fields);
        fields[0] = new StructField("rec", recursiveSet);
        Type recursiveList = new Type(Kind.LIST);
        recursiveList.setElem(recursiveStruct);
        fields[1] = new StructField("rec2", recursiveList);

        Type copy = recursiveStruct.deepCopy();
        assertEquals(recursiveStruct.getKind(), copy.getKind());
        assertFalse(recursiveStruct.getFields()[0] == copy.getFields()[0]);
        assertEquals(recursiveStruct.getFields()[0].getName(), copy.getFields()[0].getName());
        assertFalse(recursiveStruct.getFields()[0].getType() == copy.getFields()[0].getType());
        assertEquals(recursiveStruct.getFields()[0].getType().getKind(), copy.getFields()[0].getType().getKind());

        assertEquals(recursiveStruct, copy);
        assertEquals(recursiveStruct.hashCode(), copy.hashCode());
    }

    /**
     * Tests equals on equivalent types that have structural differences.
     * Here, the key and value of one map are the same type while they are different
     * but equivalent types for the other map.
     */
    public void testEqualsStructuralDifferences() {
        // Both key and elem have the same type.
        Type stringMap = new Type(Kind.MAP);
        Type recursiveSet = new Type(Kind.SET);
        recursiveSet.setKey(recursiveSet);
        stringMap.setKey(recursiveSet);
        stringMap.setElem(recursiveSet);

        // Key and elem have different but equivalent types.
        Type otherTypeStringMap = new Type(Kind.MAP);
        Type recursiveSetKey = new Type(Kind.SET);
        recursiveSetKey.setKey(recursiveSetKey);
        otherTypeStringMap.setKey(recursiveSetKey);
        Type recursiveSetElem = new Type(Kind.SET);
        recursiveSetElem.setKey(recursiveSetElem);
        otherTypeStringMap.setElem(recursiveSetElem);

        assertEquals(stringMap, otherTypeStringMap);
        assertEquals(otherTypeStringMap, stringMap);
    }
}
