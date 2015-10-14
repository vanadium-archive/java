// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.syncslides.model;

import io.v.android.apps.syncslides.db.DB;

/**
 * An implementation of DBList that doesn't do anything.  Useful for when the
 * underlying dataset has not loaded.
 */
public class NoopList<E> implements DB.DBList<E> {
    @Override
    public int getItemCount() {
        return 0;
    }

    @Override
    public E get(int i) {
        return null;
    }

    @Override
    public void setListener(Listener listener) {
    }

    @Override
    public void discard() {
    }
}

