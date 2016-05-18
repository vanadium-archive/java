// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase;

import java.util.Iterator;

public class BatchDatabase implements DatabaseHandle {
    public Id getId() {
        return null;
    }

    public Collection collection(String name, CollectionOptions opts) {
        return null;
    }

    public Collection getCollection(Id id) {
        return null;
    }

    public Iterator<Collection> getCollections() {
        return null;
    }

    public void commit() {

    }

    public void abort() {

    }
}
