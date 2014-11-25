package io.veyron.veyron.veyron2.vdl;

import java.io.Serializable;

/**
 * VdlField represents a struct or oneOf field in a VDL type.
 */
public final class VdlField implements Serializable {
    private final String name;
    private final VdlType type;

    public VdlField(String name, VdlType type) {
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
