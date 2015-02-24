package io.v.v23.vom;

import java.io.ByteArrayOutputStream;

/**
 * A stream to encode VDL values. Can discard a suffix of accumulated output.
 */
class EncodingStream extends ByteArrayOutputStream {
    int getCount() {
        return this.count;
    }

    void setCount(int count) {
        this.count = count;
    }
}
