// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.syncslides.model;

/**
 * Show and unshow a notification.
 */
public interface Notifier {
    void show(Participant p);
    void dismiss();
}
