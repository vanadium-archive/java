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
 */
public final class VdlType implements Serializable {
    private Kind kind; // used by all kinds
    private String name; // used by all kinds
    private ImmutableList<String> labels; // used by enum
    private int length; // used by array
    private VdlType key; // used by set, map
    private VdlType elem; // used by array, list, map, optional
    private ImmutableList<VdlField> fields; // used by struct and oneof

    private VdlType() {}

    public Kind getKind() {
        return kind;
    }

    public String getName() {
        return name;
    }

    public List<String> getLabels() {
        return labels;
    }

    public int getLength() {
        return length;
    }

    public VdlType getKey() {
        return key;
    }

    public VdlType getElem() {
        return elem;
    }

    public List<VdlField> getFields() {
        return fields;
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
            case ONE_OF:
            case STRUCT:
                if (type.kind == Kind.STRUCT) {
                    result += "struct{";
                } else {
                    result += "oneof{";
                }
                for (int i = 0; i < type.fields.size(); i++) {
                    if (i > 0) {
                        result += ";";
                    }
                    VdlField field = type.fields.get(i);
                    result += field.getName() + " " + typeString(field.getType(), seen);
                }
                return result + "}";
            case OPTIONAL:
                return result + "?" + typeString(type.elem, seen);
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

        if (fields != other.fields) {
            if (fields == null || this.fields.size() != other.fields.size()) {
                return false;
            }
            for (int i = 0; i < this.fields.size(); i++) {
                VdlField thisField = this.fields.get(i);
                VdlField otherField = other.fields.get(i);
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
        if (fields != null) {
            result = prime * result + this.fields.size();
            for (VdlField field : this.fields) {
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

        public PendingType optionalOf(PendingType elem) {
            return newPending(Kind.OPTIONAL).setElem(elem);
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
        private final List<VdlField> fields;
        private boolean built;

        private PendingType(VdlType vdlType) {
            this.vdlType = vdlType;
            labels = null;
            fields = null;
            built = true;
        }

        private PendingType() {
            vdlType = new VdlType();
            labels = new ArrayList<String>();
            fields = new ArrayList<VdlField>();
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
            assertOneOfKind(Kind.ARRAY, Kind.LIST, Kind.MAP, Kind.OPTIONAL);
            vdlType.elem = elem;
            return this;
        }

        public PendingType setElem(PendingType elem) {
            return setElem(elem.vdlType);
        }

        public PendingType addField(String name, VdlType type) {
            assertNotBuilt();
            assertOneOfKind(Kind.ONE_OF, Kind.STRUCT);
            fields.add(new VdlField(name, type));
            return this;
        }

        public PendingType addField(String name, PendingType type) {
            return addField(name, type.vdlType);
        }

        public PendingType assignBase(VdlType type) {
            assertNotBuilt();
            this.vdlType.kind = type.kind;
            this.vdlType.length = type.length;
            this.vdlType.key = type.key;
            this.vdlType.elem = type.elem;
            labels.clear();
            if (type.labels != null) {
                labels.addAll(type.labels);
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
