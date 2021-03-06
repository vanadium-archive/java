// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit.bind;

import rx.Subscription;

public interface RangeAdapter extends AutoCloseable {
    Subscription getSubscription();
}
