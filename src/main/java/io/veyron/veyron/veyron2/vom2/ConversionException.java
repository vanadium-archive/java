package io.veyron.veyron.veyron2.vom2;

import io.veyron.veyron.veyron2.vdl.VdlType;

import java.lang.reflect.Type;

/**
 * An exception occured during value conversion.
 */
public class ConversionException extends Exception {
    private static final long serialVersionUID = 1L;

    public ConversionException(String msg) {
        super(msg);
    }

    public ConversionException(VdlType actualType, Type targetType) {
        this("Can't convert from " + actualType + " to " + targetType);
    }

    public ConversionException(Object value, Type targetType) {
        this("Can't convert from " + value + " to " + targetType);
    }
}
