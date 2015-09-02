package io.v.v23.syncbase;

/**
 * Storage engine used for storing the syncbase data.
 */
public enum SyncbaseStorageEngine {
    LEVELDB   ("leveldb"),
    MEMSTORE  ("memstore");

    private final String value;

    SyncbaseStorageEngine(String value) {
        this.value = value;
    }

    /**
     * Returns the {@link String} value corresponding to this {@link SyncbaseStorageEngine}.
     */
    public String getValue() {
        return this.value;
    }
}
