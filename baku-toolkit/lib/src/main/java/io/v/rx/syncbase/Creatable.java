// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.rx.syncbase;

import io.v.v23.context.VContext;
import io.v.v23.security.access.Permissions;
import io.v.v23.verror.VException;
import java8.lang.FunctionalInterface;

@FunctionalInterface
interface Creatable {
    void create(VContext vContext, Permissions permissions) throws VException;
}
