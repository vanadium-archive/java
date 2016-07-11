// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase.exception;

/**
 * Thrown in response to Vanadium error: CorruptDatabase.  The client code may choose to
 * re-initialize the local Syncbase, deleting local information.
 */
public class SyncbaseRestartException extends SyncbaseException {
    SyncbaseRestartException(String message, Exception cause) {
        super(message, cause);
    }
}
