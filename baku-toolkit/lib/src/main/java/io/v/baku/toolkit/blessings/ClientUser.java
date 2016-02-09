// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit.blessings;

import lombok.Value;
import lombok.experimental.Accessors;

@Value
@Accessors(prefix = "m")
public class ClientUser {
    String mClientId, mUsername;
}
