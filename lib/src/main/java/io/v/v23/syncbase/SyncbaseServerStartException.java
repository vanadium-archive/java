// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.syncbase;

/**
 * Exception thrown if the syncbase server couldn't be started.
 */
public class SyncbaseServerStartException extends Exception {
    public SyncbaseServerStartException(String msg) {
        super(msg);
    }
}
