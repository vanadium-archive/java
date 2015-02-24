package io.v.v23.vom;

import java.io.IOException;

/**
 * CorruptVomStreamException indicates that the VOM stream is incorrectly
 * formatted.
 */
public class CorruptVomStreamException extends IOException {
    private static final long serialVersionUID = 1L;

    public CorruptVomStreamException(String message) {
        super(message);
    }
}
