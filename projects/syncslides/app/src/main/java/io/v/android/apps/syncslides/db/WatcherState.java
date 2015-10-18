// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.syncslides.db;

import android.os.Handler;
import android.os.Looper;

import io.v.v23.context.CancelableVContext;
import io.v.v23.context.VContext;
import io.v.v23.syncbase.nosql.Database;

/**
 * Holds objects that are common to all of the watchers.  The objects themselves
 * aren't actually shared.  This little class just saves on the boilerplate
 * of passing so many parameters to each watcher class.
 */
class WatcherState {
    public final CancelableVContext vContext;
    public final Database db;
    public final String deckId;
    public final String presentationId;
    public final Handler handler;
    public Thread thread;

    public WatcherState(VContext vContext, Database db, String deckId, String presentationId) {
        this.vContext = vContext.withCancel();
        this.db = db;
        this.deckId = deckId;
        this.presentationId = presentationId;
        this.handler = new Handler(Looper.getMainLooper());
    }
}
