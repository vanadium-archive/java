// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase.exception;

/**
 * Thrown in response to Vanadium errors: Timeout, or some other errors that have the RetryBackoff
 * action code. The failing method may possibly succeed if retried after a delay.
 */
public class SyncbaseRetryBackoffException extends SyncbaseException {
    SyncbaseRetryBackoffException(String message, Exception cause) {
        super(message, cause);
    }
}
