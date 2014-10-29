// TODO(bprosnitz) Either finish this or remove it before the 0.1 release.

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

        VdlType recursiveStruct = new VdlType(Kind.STRUCT);
        StructField[] fields = new StructField[2];
        recursiveStruct.setFields(fields);
        fields[0] = new StructField("rec", recursiveSet);
        VdlType recursiveList = new VdlType(Kind.LIST);
        recursiveList.setElem(recursiveStruct);
        fields[1] = new StructField("rec2", recursiveList);

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

            VdlType copy = new VdlType(type.getKind());
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

    public void testDeepCopy() {
        VdlType recursiveSet = new VdlType(Kind.SET);
        recursiveSet.setKey(recursiveSet);

        VdlType recursiveStruct = new VdlType(Kind.STRUCT);
        StructField[] fields = new StructField[2];
        recursiveStruct.setFields(fields);
        fields[0] = new StructField("rec", recursiveSet);
        VdlType recursiveList = new VdlType(Kind.LIST);
        recursiveList.setElem(recursiveStruct);
        fields[1] = new StructField("rec2", recursiveList);

        VdlType copy = recursiveStruct.deepCopy();
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
