
package com.veyron2.vom2;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

import com.veyron2.vdl.Kind;
import com.veyron2.vdl.Type;
import com.veyron2.vdl.StructField;

/**
 * TypeDecoder handles decoding types from a VOM stream and looking up types by
 * id.
 */
final class TypeDecoder {
    private final HashMap<Long, Type> definedTypes = new HashMap<Long, Type>();
    private final HashMap<Long, PartialType> partialTypes = new HashMap<Long, PartialType>();

    public TypeDecoder() {
    }

    /**
     * Look up a type by id
     *
     * @param typeId The type id
     * @return The type if found or null if not defined.
     */
    public Type lookupType(long typeId) {
        BootstrapType bt = BootstrapType.findBootstrapTypeById(typeId);
        if (bt != null) {
            return bt.getType();
        }

        return definedTypes.get(typeId);
    }

    /**
     * Define a new type with the specified type id by reading the remainder of
     * the definition off of the VOM reader.
     *
     * @param typeId The non-negative type id.
     * @param reader The reader to read from.
     * @throws CorruptVomStreamException If invalid VOM data is read.
     * @throws IOException If there is an exception reading from the stream.
     */
    public void defineType(long typeId, RawVomReader reader) throws CorruptVomStreamException,
            IOException {
        if (typeId < 0) {
            throw new RuntimeException("Invalid negative type id");
        }
        PartialType type = readType(reader);
        partialTypes.put(typeId, type);
        tryBuildPartialTypes();
    }

    private boolean allocateTypesForDependencies(long typeId,
            Map<Long, Map.Entry<PartialType, Type>> typeDeps) {
        if (lookupType(typeId) != null) {
            return true;
        }
        PartialType pt = partialTypes.get(typeId);
        if (pt == null) {
            return false;
        }
        if (typeDeps.get(typeId) != null) {
            return true;
        }
        typeDeps.put(typeId, new AbstractMap.SimpleEntry<PartialType, Type>(pt, new Type()));

        if (pt.baseTypeId != null) {
            if (!allocateTypesForDependencies(pt.baseTypeId, typeDeps)) {
                return false;
            }
        }

        if (pt.keyTypeId != null) {
            if (!allocateTypesForDependencies(pt.keyTypeId, typeDeps)) {
                return false;
            }
        }

        if (pt.elemTypeId != null) {
            if (!allocateTypesForDependencies(pt.elemTypeId, typeDeps)) {
                return false;
            }
        }

        if (pt.typeIds != null) {
            for (long id : pt.typeIds) {
                if (!allocateTypesForDependencies(id, typeDeps)) {
                    return false;
                }
            }
        }

        if (pt.fields != null) {
            for (PartialStructField sf : pt.fields) {
                if (!allocateTypesForDependencies(sf.typeId, typeDeps)) {
                    return false;
                }
            }
        }

        return true;
    }

    private boolean tryBuildPartialType(long typeId, PartialType pt) {
        Map<Long, Map.Entry<PartialType, Type>> types = new HashMap<Long, Map.Entry<PartialType, Type>>();
        if (!allocateTypesForDependencies(typeId, types)) {
            return false;
        }

        for (Map.Entry<Long, Map.Entry<PartialType, Type>> entry : types.entrySet()) {
            long entryTypeId = entry.getKey();
            PartialType entryPt = entry.getValue().getKey();
            Type entryType = entry.getValue().getValue();

            if (pt.baseTypeId != null) {
                // named primitive
                Type ty = lookupType(pt.baseTypeId);
                if (ty == null) {
                    ty = types.get(pt.baseTypeId).getValue();
                }
                if (ty == null) {
                    throw new RuntimeException("Unexpectedly failed to find type");
                }
                Type copy = ty.shallowCopy();
                copy.setName(pt.name);
                partialTypes.remove(typeId);
                definedTypes.put(typeId, copy);
                continue;
            }

            entryType.setKind(entryPt.kind);
            entryType.setName(entryPt.name);
            entryType.setLabels(entryPt.labels);
            entryType.setLength(entryPt.length);

            if (entryPt.keyTypeId != null) {
                Type keyTy = lookupType(entryPt.keyTypeId);
                if (keyTy == null) {
                    keyTy = types.get(entryPt.keyTypeId).getValue();
                }
                if (keyTy == null) {
                    throw new RuntimeException("Unexpectedly failed to find type");
                }
                entryType.setKey(keyTy);
            }

            if (entryPt.elemTypeId != null) {
                Type elemTy = lookupType(entryPt.elemTypeId);
                if (elemTy == null) {
                    elemTy = types.get(entryPt.elemTypeId).getValue();
                }
                if (elemTy == null) {
                    throw new RuntimeException("Unexpectedly failed to find type");
                }
                entryType.setElem(elemTy);
            }

            if (entryPt.typeIds != null) {
                Type[] oneOfTypes = new Type[entryPt.typeIds.length];
                for (int i = 0; i < entryPt.typeIds.length; i++) {
                    long id = entryPt.typeIds[i];
                    Type aTy = lookupType(id);
                    if (aTy == null) {
                        aTy = types.get(id).getValue();
                    }
                    if (aTy == null) {
                        throw new RuntimeException("Unexpectedly failed to find type");
                    }
                    oneOfTypes[i] = aTy;
                }
                entryType.setTypes(oneOfTypes);
            }

            if (entryPt.fields != null) {
                StructField[] fields = new StructField[entryPt.fields.length];
                for (int i = 0; i < entryPt.fields.length; i++) {
                    PartialStructField partialFld = entryPt.fields[i];
                    Type fieldTy = lookupType(partialFld.typeId);
                    if (fieldTy == null) {
                        fieldTy = types.get(partialFld.typeId).getValue();
                    }
                    if (fieldTy == null) {
                        throw new RuntimeException("Unexpectedly failed to find type");
                    }
                    fields[i] = new StructField(partialFld.name, fieldTy);
                }
                entryType.setFields(fields);
            }

            definedTypes.put(entryTypeId, entryType);
            partialTypes.remove(entryTypeId);
        }

        return true;
    }

    private void tryBuildPartialTypes() {
        boolean progress = true;
        while (!partialTypes.isEmpty() && progress) {
            for (Map.Entry<Long, PartialType> entry : partialTypes.entrySet()) {
                progress = tryBuildPartialType(entry.getKey(), entry.getValue());
                break;
            }
        }
    }

    // NOTE: this doesn't currently support recursive types or types that are
    // defined out of order!
    private PartialType readType(RawVomReader reader) throws CorruptVomStreamException, IOException {
        int wiretypeKind = (int) reader.readUint();
        switch (BootstrapType.findBootstrapTypeById(wiretypeKind)) {
            case WIRE_NAMED: {
                long nextIndex = reader.readUint();
                String name = null;
                long baseTypeId = 0;
                while (nextIndex != 0) {
                    switch ((int) nextIndex) {
                        case 1:
                            name = reader.readString();
                            break;
                        case 2:
                            baseTypeId = reader.readUint();
                            break;
                    }
                    nextIndex = reader.readUint();
                }
                PartialType pt = new PartialType();
                pt.name = name;
                pt.baseTypeId = baseTypeId;
                return pt;
            }
            case WIRE_ENUM: {
                throw new RuntimeException("Not yet implemented");
            }
            case WIRE_ARRAY: {
                long nextIndex = reader.readUint();
                String name = null;
                Long elemTypeId = null;
                long length = 0;
                while (nextIndex != 0) {
                    switch ((int) nextIndex) {
                        case 1:
                            name = reader.readString();
                            break;
                        case 2:
                            elemTypeId = reader.readUint();
                            break;
                        case 3:
                            length = reader.readUint();
                    }
                    nextIndex = reader.readUint();
                }
                PartialType pt = new PartialType();
                pt.kind = Kind.ARRAY;
                pt.length = (int) length;
                pt.elemTypeId = elemTypeId;
                pt.name = name;
                return pt;
            }
            case WIRE_LIST: {
                long nextIndex = reader.readUint();
                String name = null;
                Long elemTypeId = null;
                while (nextIndex != 0) {
                    switch ((int) nextIndex) {
                        case 1:
                            name = reader.readString();
                            break;
                        case 2:
                            elemTypeId = reader.readUint();
                            break;
                    }
                    nextIndex = reader.readUint();
                }
                PartialType pt = new PartialType();
                pt.kind = Kind.LIST;
                pt.elemTypeId = elemTypeId;
                pt.name = name;
                return pt;
            }
            case WIRE_SET: {
                long nextIndex = reader.readUint();
                String name = null;
                Long elemTypeId = null;
                while (nextIndex != 0) {
                    switch ((int) nextIndex) {
                        case 1:
                            name = reader.readString();
                            break;
                        case 2:
                            elemTypeId = reader.readUint();
                            break;
                    }
                    nextIndex = reader.readUint();
                }
                PartialType pt = new PartialType();
                pt.kind = Kind.SET;
                pt.keyTypeId = elemTypeId;
                pt.name = name;
                return pt;
            }
            case WIRE_MAP: {
                long nextIndex = reader.readUint();
                String name = null;
                Long keyTypeId = null;
                Long elemTypeId = null;
                while (nextIndex != 0) {
                    switch ((int) nextIndex) {
                        case 1:
                            name = reader.readString();
                            break;
                        case 2:
                            keyTypeId = reader.readUint();
                            break;
                        case 3:
                            elemTypeId = reader.readUint();
                            break;
                    }
                    nextIndex = reader.readUint();
                }
                PartialType pt = new PartialType();
                pt.kind = Kind.MAP;
                pt.keyTypeId = keyTypeId;
                pt.elemTypeId = elemTypeId;
                pt.name = name;
                return pt;
            }
            case WIRE_STRUCT: {
                long nextIndex = reader.readUint();
                String name = null;
                PartialStructField[] fields = null;
                while (nextIndex != 0) {
                    switch ((int) nextIndex) {
                        case 1:
                            name = reader.readString();
                            break;
                        case 2:
                            int numFields = (int) reader.readUint();
                            fields = new PartialStructField[numFields];
                            for (int i = 0; i < numFields; i++) {
                                String fieldName = null;
                                Long fieldTypeId = null;
                                long nextFieldIndex = reader.readUint();
                                while (nextFieldIndex != 0) {
                                    switch ((int) nextFieldIndex) {
                                        case 1:
                                            fieldName = reader.readString();
                                            break;
                                        case 2:
                                            fieldTypeId = reader.readUint();
                                            break;
                                    }
                                    nextFieldIndex = reader.readUint();
                                }
                                fields[i] = new PartialStructField(fieldName, fieldTypeId);
                            }
                            break;
                    }
                    nextIndex = reader.readUint();
                }
                PartialType pt = new PartialType();
                pt.kind = Kind.STRUCT;
                pt.fields = fields;
                pt.name = name;
                return pt;
            }
            case WIRE_ONE_OF:
                throw new RuntimeException("Not yet implemented");
            default:
                throw new CorruptVomStreamException("Unknown wiretype kind: " + wiretypeKind);
        }
    }

    private static final class PartialType {
        public Long baseTypeId; // used by named

        public Kind kind; // used by all kinds
        public String name; // used by all kinds
        public String[] labels; // used by enum
        public int length; // used by array
        public Long keyTypeId; // used by set, map
        public Long elemTypeId; // used by array, list, map
        public Long[] typeIds; // used by oneof
        public PartialStructField[] fields; // used by struct

        public PartialType() {
        }
    }

    private static final class PartialStructField {
        String name;
        long typeId;

        PartialStructField(String name, long typeId) {
            this.name = name;
            this.typeId = typeId;
        }
    }
}
