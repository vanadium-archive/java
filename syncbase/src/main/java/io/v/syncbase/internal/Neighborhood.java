// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase.internal;

import java.util.List;

import io.v.syncbase.core.NeighborhoodPeer;
import io.v.syncbase.core.VError;

public class Neighborhood {
    public static native void StartAdvertising(List<String> visibility) throws VError;
    public static native void StopAdvertising();
    public static native boolean IsAdvertising();

    public interface NeighborhoodScanCallbacks {
        void onPeer(NeighborhoodPeer peer);
    }

    public static native long NewScan(NeighborhoodScanCallbacks callbacks) throws VError;
    public static native void StopScan(long id);
}
