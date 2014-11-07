package io.veyron.veyron.veyron2.vdl;

import com.google.common.collect.ImmutableList;

import java.io.Serializable;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/**
 * Type represents VDL types. TODO(bprosnitz) This is currently mutable.
 * Consider adding type builders and making these objects immutable.
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

    public VdlType shallowCopy() {
        VdlType copy = new VdlType(this.kind);
        copy.name = this.name;
        copy.labels = this.labels;
        copy.length = this.length;
        copy.key = this.key;
        copy.elem = this.elem;
        copy.types = this.types;
        copy.fields = this.fields;
        return copy;
    }
}
