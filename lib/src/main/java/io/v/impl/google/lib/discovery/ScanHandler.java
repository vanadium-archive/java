// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.impl.google.lib.discovery;

import io.v.x.ref.lib.discovery.Advertisement;

/**
 * An interface that is passed into a Vanadium Discovery Scan operation that will handle updates.
 */
public interface ScanHandler {
    /**
     * Called when there is a new advertisement or an update to and old advertisement
     */
    void handleUpdate(Advertisement advertisement);
}


