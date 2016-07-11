// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase.exception;

/**
 * A Syncbase error that the client code might want to handle, depending on subclass thrown.
 */
public abstract class SyncbaseException extends Exception {
    SyncbaseException(String message, Exception cause) {
        super(message, cause);
    }
}
