// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase;

import java.util.Iterator;

public interface DatabaseHandle {
    Id getId();

    class CollectionOptions {
        public boolean withoutSyncgroup;
    }

    Collection collection(String name, CollectionOptions opts);

    Collection getCollection(Id id);

    Iterator<Collection> getCollections();
}
