package io.veyron.veyron.veyron2.vdl;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/**
 * Type represents VDL types.
 * TODO(rogulenko): make this immutable
 */
public final class VdlType implements Serializable {
    private Kind kind; // used by all kinds
    private String name; // used by all kinds
    private ImmutableList<String> labels; // used by enum
    private int length; // used by array
    private VdlType key; // used by set, map
    private VdlType elem; // used by array, list, map
    private ImmutableList<VdlType> types; // used by oneof
    private ImmutableList<VdlStructField> fields; // used by struct

    public VdlType(Kind kind) {
        this.kind = kind;
    }

    public VdlType() {
    }

    public Kind getKind() {
        return kind;
    }

    public void setKind(Kind kind) {
        this.kind = kind;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getLabels() {
        return labels;
    }

    public void setLabels(String... labels) {
        this.labels = ImmutableList.copyOf(labels);
    }

    public void setLabels(List<String> labels) {
        this.labels = ImmutableList.copyOf(labels);
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public VdlType getKey() {
        return key;
    }

    public void setKey(VdlType key) {
        this.key = key;
    }

    public VdlType getElem() {
        return elem;
    }

    public void setElem(VdlType elem) {
        this.elem = elem;
    }

    public List<VdlType> getTypes() {
        return types;
    }

    public void setTypes(VdlType... types) {
        this.types = ImmutableList.copyOf(types);
    }

    public void setTypes(List<VdlType> types) {
        this.types = ImmutableList.copyOf(types);
    }

    public List<VdlStructField> getFields() {
        return fields;
    }

    public void setFields(VdlStructField... fields) {
        this.fields = ImmutableList.copyOf(fields);
    }

    public void setFields(List<VdlStructField> fields) {
        this.fields = ImmutableList.copyOf(fields);
    }

    private static String typeString(VdlType type,
            final java.util.IdentityHashMap<VdlType, Boolean> seen) {
        if (seen.containsKey(type) && type.name != null) {
            return type.name;
        }
        seen.put(type, true);
        String result = "";
        if (type.name != null) {
            result = type.name + " ";
        }
        switch (type.kind) {
            case ENUM:
                return result + "enum{" + Joiner.on(";").join(type.labels) + "}";
            case ARRAY:
                return result + "[" + type.length + "]" + typeString(type.elem, seen);
            case LIST:
                return result + "[]" + typeString(type.elem, seen);
            case SET:
                return result + "set[" + typeString(type.key, seen) + "]";
            case MAP:
                return result + "map[" + typeString(type.key, seen) + "]"
                        + typeString(type.elem, seen);
            case STRUCT:
                result += "struct{";
                for (int i = 0; i < type.fields.size(); i++) {
                    if (i > 0) {
                        result += ";";
                    }
                    VdlStructField field = type.fields.get(i);
                    result += field.getName() + " " + typeString(field.getType(), seen);
                }
                return result + "}";
            case ONE_OF:
                result += "oneof{";
                for (int i = 0; i < type.types.size(); i++) {
                    if (i > 0) {
                        result += ";";
                    }
                    result += typeString(type.types.get(i), seen);
                }
                return result + "}";
            default:
                return result + type.kind.name().toLowerCase();
            }
    }

    private boolean equal(Object a, Object b) {
        return a == b || (a != null && a.equals(b));
    }

    private boolean recursiveEquals(final VdlType other,
            final IdentityHashMap<Object, Set<Object>> seen) {
        Set<Object> matches;
        if (seen.containsKey(this)) {
            matches = seen.get(this);
            if (matches.contains(other)) {
                return true;
            }
        } else {
            matches = new HashSet<Object>();
            seen.put(this, matches);
        }
        matches.add(other);

        if (!equal(kind, other.kind) || !equal(name, other.name) || !equal(labels, other.labels)
                || length != other.length) {
            return false;
        }
        if (key != other.key && (key == null || !key.recursiveEquals(other.key, seen))) {
            return false;
        }
        if (elem != other.elem && (elem == null || !elem.recursiveEquals(other.elem, seen))) {
            return false;
        }

        if (types != other.types) {
            if (types == null || types.size() != other.types.size()) {
                return false;
            }
            for (int i = 0; i < this.types.size(); i++) {
                if (!this.types.get(i).recursiveEquals(other.types.get(i), seen)) {
                    return false;
                }
            }
        }

        if (fields != other.fields) {
            if (fields == null || this.fields.size() != other.fields.size()) {
                return false;
            }
            for (int i = 0; i < this.fields.size(); i++) {
                VdlStructField thisField = this.fields.get(i);
                VdlStructField otherField = other.fields.get(i);
                if (!thisField.getName().equals(otherField.getName())
                        || !thisField.getType().recursiveEquals(otherField.getType(), seen)) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (this.getClass() != other.getClass()) {
            return false;
        }
        return recursiveEquals((VdlType) other, new IdentityHashMap<Object, Set<Object>>());
    }

    private int recursiveHashCode(IdentityHashMap<Object, Void> seen) {
        if (seen.containsKey(this)) {
            return 0;
        }
        seen.put(this, null);
        int result = 1;
        final int prime = 31;
        result = prime * result + this.kind.hashCode();
        result = prime * result
                + (this.name == null ? 0 : this.name.hashCode());
        result = prime * result
                + (this.labels == null ? 0 : this.labels.hashCode());
        result = prime * result + this.length;
        result = prime * result
                + (this.key == null ? 0 : this.key.recursiveHashCode(seen));
        result = prime * result
                + (this.elem == null ? 0 : this.elem.recursiveHashCode(seen));
        if (types != null) {
            result = prime * result + this.types.size();
            for (VdlType type : types) {
                result = prime * result + type.recursiveHashCode(seen);
            }
        } else {
            result = prime * result;
        }
        if (fields != null) {
            result = prime * result + this.fields.size();
            for (VdlStructField field : this.fields) {
                result = prime * result + field.getName().hashCode();
                result = prime * result + field.getType().recursiveHashCode(seen);
            }
        }
        return result;
    }

    @Override
    public int hashCode() {
        return recursiveHashCode(new IdentityHashMap<Object, Void>());
    }

    @Override
    public String toString() {
        return typeString(this, new IdentityHashMap<VdlType, Boolean>());
    }

    public VdlType shallowCopy() {
        VdlType copy = new VdlType();
        copy.kind = this.kind;
        copy.name = this.name;
        copy.labels = this.labels;
        copy.length = this.length;
        copy.key = this.key;
        copy.elem = this.elem;
        copy.types = this.types;
        copy.fields = this.fields;
        return copy;
    }

    /**
     * Builder builds Types. There are two phases: 1) Create PendingType objects and describe each
     * type, and 2) call build(). When build() is called, all types are created and may be retrieved
     * by calling built() on the pending type. This two-phase building enables support for recursive
     * types, and also makes it easy to construct a group of dependent types without determining
     * their dependency ordering.
     */
    public static final class Builder {
        private final List<PendingType> pendingTypes;

        public Builder() {
            pendingTypes = new ArrayList<PendingType>();
        }

        public PendingType newPending() {
            PendingType type = new PendingType();
            pendingTypes.add(type);
            return type;
        }

        public PendingType newPending(Kind kind) {
            return newPending().setKind(kind);
        }

        public PendingType listOf(PendingType elem) {
            return newPending(Kind.LIST).setElem(elem);
        }

        public PendingType setOf(PendingType key) {
            return newPending(Kind.SET).setKey(key);
        }

        public PendingType mapOf(PendingType key, PendingType elem) {
            return newPending(Kind.MAP).setKey(key).setElem(elem);
        }

        public PendingType builtPendingFromType(VdlType vdlType) {
            return new PendingType(vdlType);
        }

        public void build() {
            for (PendingType type : pendingTypes) {
                type.build();
            }
            pendingTypes.clear();
        }
    }

    public static final class PendingType {
        private final VdlType vdlType;
        private final List<String> labels;
        private final List<VdlType> types;
        private final List<VdlStructField> fields;
        private boolean built;

        private PendingType(VdlType vdlType) {
            this.vdlType = vdlType;
            labels = null;
            types = null;
            fields = null;
            built = true;
        }

        private PendingType() {
            vdlType = new VdlType();
            labels = new ArrayList<String>();
            types = new ArrayList<VdlType>();
            fields = new ArrayList<VdlStructField>();
            built = false;
        }

        private void build() {
            if (built) {
                return;
            }
            switch (vdlType.kind) {
                case ENUM:
                    vdlType.labels = ImmutableList.copyOf(labels);
                    break;
                case ONE_OF:
                    vdlType.types = ImmutableList.copyOf(types);
                    break;
                case STRUCT:
                    vdlType.fields = ImmutableList.copyOf(fields);
                    break;
                default:
                    // do nothing
            }
            built = true;
        }

        public PendingType setKind(Kind kind) {
            assertNotBuilt();
            vdlType.kind = kind;
            return this;
        }

        public PendingType setName(String name) {
            assertNotBuilt();
            vdlType.name = name;
            return this;
        }

        public PendingType addLabel(String label) {
            assertNotBuilt();
            assertOneOfKind(Kind.ENUM);
            labels.add(label);
            return this;
        }

        public PendingType setLength(int length) {
            assertNotBuilt();
            assertOneOfKind(Kind.ARRAY);
            vdlType.length = length;
            return this;
        }

        public PendingType setKey(VdlType key) {
            assertNotBuilt();
            assertOneOfKind(Kind.SET, Kind.MAP);
            vdlType.key = key;
            return this;
        }

        public PendingType setKey(PendingType key) {
            return setKey(key.vdlType);
        }

        public PendingType setElem(VdlType elem) {
            assertNotBuilt();
            assertOneOfKind(Kind.ARRAY, Kind.LIST, Kind.MAP);
            vdlType.elem = elem;
            return this;
        }

        public PendingType setElem(PendingType elem) {
            return setElem(elem.vdlType);
        }

        public PendingType addType(VdlType type) {
            assertNotBuilt();
            assertOneOfKind(Kind.ONE_OF);
            types.add(type);
            return this;
        }

        public PendingType addType(PendingType type) {
            return addType(type.vdlType);
        }

        public PendingType addField(String name, VdlType type) {
            assertNotBuilt();
            assertOneOfKind(Kind.STRUCT);
            fields.add(new VdlStructField(name, type));
            return this;
        }

        public PendingType addField(String name, PendingType type) {
            return addField(name, type.vdlType);
        }

        public PendingType assignBase(VdlType type) {
            assertNotBuilt();
            this.vdlType.kind = type.kind;
            this.vdlType.name = type.name;
            this.vdlType.length = type.length;
            this.vdlType.key = type.key;
            this.vdlType.elem = type.elem;
            labels.clear();
            if (type.labels != null) {
                labels.addAll(type.labels);
            }
            types.clear();
            if (type.types != null) {
                types.addAll(type.types);
            }
            fields.clear();
            if (type.fields != null) {
                fields.addAll(type.fields);
            }
            return this;
        }

        public PendingType assignBase(PendingType pending) {
            return assignBase(pending.vdlType);
        }

        public VdlType built() {
            if (!built) {
                throw new IllegalStateException("The pending type is not built yet");
            }
            return vdlType;
        }

        private void assertNotBuilt() {
            if (built) {
                throw new IllegalStateException("The pending type is already built");
            }
        }

        private void assertOneOfKind(Kind... kinds) {
            for (Kind kind : kinds) {
                if (vdlType.kind == kind) {
                    return;
                }
            }
            throw new IllegalArgumentException("Unsupported operation for kind " + vdlType.kind);
        }
    }
}
