// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.v23.services.blessing;

/**
 * Exception thrown on blessing creation error.
 */
public class BlessingCreationException extends Exception {
    public BlessingCreationException(String msg) {
        super(msg);
    }
}
