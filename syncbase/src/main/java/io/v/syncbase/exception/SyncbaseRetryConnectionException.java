// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase.exception;

/**
 * Thrown in response to some other errors that have the RetryConnection action code. The failing
 * method may possibly succeed if retried after the connection is re-established.
 */
public class SyncbaseRetryConnectionException extends SyncbaseException {
    SyncbaseRetryConnectionException(String message, Exception cause) {
        super(message, cause);
    }
}
