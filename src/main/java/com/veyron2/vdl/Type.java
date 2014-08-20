// TODO(bprosnitz) Either finish this or remove it before the 0.1 release.

package com.veyron2.vdl;

import java.util.Arrays;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * Type represents VDL types. TODO(bprosnitz) This is currently mutable.
 * Consider adding type builders and making these objects immutable.
 */
public final class Type {
    private Kind kind; // used by all kinds
    private String name; // used by all kinds
    private String[] labels; // used by enum
    private int length; // used by array
    private Type key; // used by set, map
    private Type elem; // used by array, list, map
    private Type[] types; // used by oneof
    private StructField[] fields; // used by struct

    public Type(Kind kind) {
        this.kind = kind;
    }

    public Type() {
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

    public String[] getLabels() {
        return labels;
    }

    public void setLabels(String[] labels) {
        this.labels = labels;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public Type getKey() {
        return key;
    }

    public void setKey(Type key) {
        this.key = key;
    }

    public Type getElem() {
        return elem;
    }

    public void setElem(Type elem) {
        this.elem = elem;
    }

    public Type[] getTypes() {
        return types;
    }

    public void setTypes(Type... types) {
        this.types = types;
    }

    public StructField[] getFields() {
        return fields;
    }

    public void setFields(StructField... fields) {
        this.fields = fields;
    }

    private boolean recursiveEquals(final Type other,
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

        if (this.kind != other.kind) {
            return false;
        }

        if (this.name != null) {
            if (!this.name.equals(other.name)) {
                return false;
            }
        } else if (other.name != null) {
            return false;
        }

        if (!Arrays.equals(this.labels, other.labels)) {
            return false;
        }

        if (this.length != other.length) {
            return false;
        }

        if (this.key != null) {
            if (other.key == null) {
                return false;
            }
            if (!this.key.recursiveEquals(other.key, seen)) {
                return false;
            }
        } else if (other.key != null) {
            return false;
        }

        if (this.elem != null) {
            if (other.elem == null) {
                return false;
            }
            if (!this.elem.recursiveEquals(other.elem, seen)) {
                return false;
            }
        } else if (other.elem != null) {
            return false;
        }

        if (this.types != null) {
            if (this.types.length != other.types.length) {
                return false;
            }
            for (int i = 0; i < this.types.length; i++) {
                if (!this.types[i].recursiveEquals(other.types[i], seen)) {
                    return false;
                }
            }
        } else if (other.types != null) {
            return false;
        }

        if (this.fields != null) {
            if (other.fields == null) {
                return false;
            }
            if (this.fields.length != other.fields.length) {
                return false;
            }
            for (int i = 0; i < this.fields.length; i++) {
                StructField thisField = this.fields[i];
                StructField otherField = other.fields[i];
                if (!thisField.getName().equals(otherField.getName())) {
                    return false;
                }

                if (!thisField.getType().recursiveEquals(otherField.getType(), seen)) {
                    return false;
                }
            }
        } else if (other.fields != null) {
            return false;
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
        return recursiveEquals((Type) other, new IdentityHashMap<Object, Set<Object>>());
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
            result = prime * result + this.types.length;
            for (Type type : types) {
                result = prime * result + type.hashCode();
            }
        } else {
            result = prime * result;
        }
        if (fields != null) {
            result = prime * result + this.fields.length;
            for (StructField field : this.fields) {
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

    private Type recursiveDeepCopy(IdentityHashMap<Object, Type> seen) {
        if (seen.get(this) != null) {
            return seen.get(this);
        }
        Type copy = new Type(this.kind);
        seen.put(this, copy);

        copy.name = this.name;
        copy.labels = this.labels;
        copy.length = this.length;
        if (this.key != null) {
            copy.key = this.key.recursiveDeepCopy(seen);
        }
        if (this.elem != null) {
            copy.elem = this.elem.recursiveDeepCopy(seen);
        }
        if (this.types != null) {
            copy.types = new Type[this.types.length];
            for (int i = 0; i < this.types.length; i++) {
                copy.types[i] = this.types[i].recursiveDeepCopy(seen);
            }
        }
        if (this.fields != null) {
            copy.fields = new StructField[this.fields.length];
            for (int i = 0; i < this.fields.length; i++) {
                copy.fields[i] = new StructField(this.fields[i].getName(),
                        this.fields[i].getType().recursiveDeepCopy(seen));
            }
        }
        return copy;
    }

    Type deepCopy() {
        return recursiveDeepCopy(new IdentityHashMap<Object, Type>());
    }

    public Type shallowCopy() {
        Type copy = new Type(this.kind);
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
