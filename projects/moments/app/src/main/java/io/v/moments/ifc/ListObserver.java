// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.moments.ifc;

public interface ListObserver {
    void notifyItemInserted(int position);

    void notifyItemChanged(int position);

    void notifyItemRemoved(int position);
}
