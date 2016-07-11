// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase.exception;

/**
 * An internal error within Syncbase.
 */
public class SyncbaseInternalException extends RuntimeException {
    public SyncbaseInternalException(String message) {
        super(message);
    }

    SyncbaseInternalException(String message, Exception cause) {
        super(message, cause);
    }
}
