package io.v.core.veyron2.vom;

import java.lang.reflect.Type;

/**
 * An exception occured during value conversion.
 */
public class ConversionException extends Exception {
    private static final long serialVersionUID = 1L;

    public ConversionException(String msg) {
        super(msg);
    }

    public ConversionException(Object value, Type targetType) {
        this("Can't convert from " + value + " to " + targetType);
    }

    public ConversionException(Object value, Type targetType, String cause) {
        this("Can't convert from " + value + " to " + targetType + " : " + cause);
    }
}
