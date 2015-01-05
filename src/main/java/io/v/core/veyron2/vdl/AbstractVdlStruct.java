package io.v.core.veyron2.vdl;

/**
 * AbstractVdlStruct is a basic class for values of type VDL struct.
 * It is used to get VDL type from classes through reflection.
 */
public abstract class AbstractVdlStruct extends VdlValue {
    public AbstractVdlStruct(VdlType type) {
        super(type);
        assertKind(Kind.STRUCT);
    }
}
