package io.veyron.veyron.veyron2.vdl;

import java.io.Serializable;

/**
 * Value is the generic representation of any value expressible in veyron.  All values are typed.
 */
public abstract class VdlValue implements Serializable {
    private final Type type;

    protected VdlValue(Type type) {
        this.type = type;
    }

    /**
     * Returns the runtime vdl type of this value.
     *
     * @return The {@code Type} object that represents the runtime
     *         vdl type of this vdl value.
     */
    public Type getType() {
        return type;
    }
}
