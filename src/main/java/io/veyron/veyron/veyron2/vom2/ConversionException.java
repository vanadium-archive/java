package io.veyron.veyron.veyron2.vom2;

/**
 * An exception occured during VOM type conversion.
 */
public class ConversionException extends Exception {
    private static final long serialVersionUID = 1L;

    public ConversionException(String msg) {
        super(msg);
    }
}
