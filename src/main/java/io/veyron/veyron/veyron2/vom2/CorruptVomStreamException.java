// TODO(bprosnitz) Either finish this or remove it before the 0.1 release.

package io.veyron.veyron.veyron2.vom2;

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
