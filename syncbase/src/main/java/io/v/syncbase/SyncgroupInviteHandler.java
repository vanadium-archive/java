// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase;

// TODO(sadovsky): Make this a nested class of Database?
public abstract class SyncgroupInviteHandler {
    void onInvite(SyncgroupInvite invite) {

    }

    void onError(Exception e) {

    }
}
