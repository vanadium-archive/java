// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase.exception;

/**
 * Thrown in response to Vanadium errors: NoServers or SyncgroupJoinFailed.
 */
public class SyncbaseNoServersException extends SyncbaseException {
    SyncbaseNoServersException(String message, Exception cause) {
        super(message, cause);
    }
}
