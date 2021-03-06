// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.syncbase;

/**
 * Represents a change to a row in a collection.
 */
public enum ChangeType {
    /**
     * A change that was a result of a value being {@link Row#put put} into a collection row.
     */
    PUT_CHANGE,
    /**
     * A change that was a result of a row {@link Row#delete deletion}.
     */
    DELETE_CHANGE;
}
