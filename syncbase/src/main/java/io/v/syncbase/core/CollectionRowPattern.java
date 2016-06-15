// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase.core;

public class CollectionRowPattern {
    public String collectionBlessing;
    public String collectionName;
    public String rowKey;

    public CollectionRowPattern() {
    }

    public CollectionRowPattern(String collectionBlessing, String collectionName, String rowKey) {
        this.collectionBlessing = collectionBlessing;
        this.collectionName = collectionName;
        this.rowKey = rowKey;
    }
}