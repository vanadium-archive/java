// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.syncbase.nosql;

import io.v.v23.vdl.VdlAny;
import io.v.v23.verror.VException;

import java.util.List;

/**
 * An interface for iterating through rows resulting from a
 * {@link DatabaseCore#exec DatabaseCore.exec()}.
 */
public interface ResultStream extends Stream<List<VdlAny>> {
    /**
     * Returns an array of column names that matched the query.  The size of the {@link VdlAny}
     * list returned in every iteration will match the size of this array.
     */
    List<String> columnNames();
}