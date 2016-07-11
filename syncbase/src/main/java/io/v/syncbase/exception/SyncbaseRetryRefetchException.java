// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase.exception;

/**
 * Thrown in response to some other errors that have the RetryRefetch action code. The failing
 * method may possibly succeed if retried after fetching update versions of the parameters.
 */
public class SyncbaseRetryRefetchException extends SyncbaseException {
    SyncbaseRetryRefetchException(String message, Exception cause) {
        super(message, cause);
    }
}
