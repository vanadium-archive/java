package io.veyron.veyron.veyron2.vdl;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates the VDL name this entity is generated from.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.FIELD})
public @interface GeneratedFromVdlName {
    /**
     * The name of the VDL entity from which this entity was generated.
     */
    String value();
}
