// TODO(bprosnitz) Either finish this or remove it before the 0.1 release.

package io.veyron.veyron.veyron2.vdl;

/**
 * StructField represents a struct field in a VDL type.
 */
public final class StructField {
    private String name;
    private Type type;

    public StructField(String name, Type type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public Type getType() {
        return type;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setType(Type type) {
        this.type = type;
    }
}
