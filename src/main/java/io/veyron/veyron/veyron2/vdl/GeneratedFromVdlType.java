package io.veyron.veyron.veyron2.vdl;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that this class is generated from VDL.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface GeneratedFromVdlType {
    /**
     * The name of the type in VDL from which this class was generated.
     */
    String value();
}
