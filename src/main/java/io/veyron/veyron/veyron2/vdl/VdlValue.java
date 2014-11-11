package io.veyron.veyron.veyron2.vdl;

import java.io.Serializable;

/**
 * Value is the generic representation of any value expressible in veyron.  All values are typed.
 */
public abstract class VdlValue implements Serializable {
    private final VdlType type;

    protected VdlValue(VdlType type) {
        this.type = type;
    }

    protected void assertKind(Kind kind) {
        if (type.getKind() != kind) {
            throw new IllegalArgumentException("Kind of VDL type should be " + kind);
        }
    }

    /**
     * Returns the runtime VDL type of this value.
     *
     * @return The {@code Type} object that represents the runtime
     *         VDL type of this VDL value.
     */
    public VdlType vdlType() {
        return type;
    }
}
