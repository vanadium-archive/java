package io.veyron.veyron.veyron2.vdl;

import junit.framework.TestCase;

/**
 * Tests methods on VDL Types.
 */
public class TypeTest extends TestCase {
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
