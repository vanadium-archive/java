// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase;

import java.util.Iterator;

public class BatchDatabase implements DatabaseHandle {
    public Id getId() {
        throw new RuntimeException("Not implemented");
    }

    public Collection collection(String name, CollectionOptions opts) {
        throw new RuntimeException("Not implemented");
    }

    public Collection getCollection(Id id) {
        throw new RuntimeException("Not implemented");
    }

    public Iterator<Collection> getCollections() {
        throw new RuntimeException("Not implemented");
    }

    public void commit() {
        throw new RuntimeException("Not implemented");
    }

    public void abort() {
        throw new RuntimeException("Not implemented");
    }
}
