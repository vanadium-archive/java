// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase.exception;

/**
 * Thrown in response to various inconsistencies in permissions, or unsupported authentication
 * provider, or Vanadium errors: NoAccess, NotTrusted, NoExistOrNoAccess, UnauthorizedCreateId,
 * InferAppBlessingFailed, InferUserBlessingFailed, or InferDefaultPermsFailed.
 */
public class SyncbaseSecurityException extends SyncbaseException {
    SyncbaseSecurityException(String message, Exception cause) {
        super(message, cause);
    }
}
