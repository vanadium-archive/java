// TODO(bprosnitz) Either finish this or remove it before the 0.1 release.

package io.veyron.veyron.veyron2.vdl;

import java.io.Serializable;

/**
 * StructField represents a struct field in a VDL type.
 */
public final class StructField implements Serializable {
    private String name;
    private VdlType type;

    public StructField(String name, VdlType type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public VdlType getType() {
        return type;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setType(VdlType type) {
        this.type = type;
    }
}
