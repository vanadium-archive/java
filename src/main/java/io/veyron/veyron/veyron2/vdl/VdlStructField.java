package io.veyron.veyron.veyron2.vdl;

import java.io.Serializable;

/**
 * StructField represents a struct field in a VDL type.
 */
public final class VdlStructField implements Serializable {
    private final String name;
    private final VdlType type;

    public VdlStructField(String name, VdlType type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public VdlType getType() {
        return type;
    }
}
