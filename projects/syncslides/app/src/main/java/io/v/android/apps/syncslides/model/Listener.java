// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.syncslides.model;

/**
 * Callbacks for when the dataset changes dynamically.
 */
public interface Listener {
    void notifyItemChanged(int position);
    void notifyItemInserted(int position);
    void notifyItemRemoved(int position);
}
