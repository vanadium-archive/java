// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

/**
 * For each {@linkplain io.v.rx.syncbase.SyncbaseEntity RxSyncbase entity} class, this package adds
 * a {@link io.v.baku.toolkit.BakuActivityTrait} as context information from which to derive other
 * dependencies and to use for error handling. This allows Baku client code to use Syncbase more
 * easily, without needing to explicitly subscribe error handlers for every operation. For more
 * information, see {@link io.v.baku.toolkit.syncbase.BakuTable#exec(rx.functions.Func1)}.
 */
package io.v.baku.toolkit.syncbase;