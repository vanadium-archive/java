// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit.syncbase;

import io.v.baku.toolkit.BakuActivityTrait;
import io.v.rx.syncbase.RxDb;
import lombok.Getter;
import lombok.experimental.Accessors;

@Accessors(prefix = "m")
@Getter
public class BakuDb extends RxDb {
    private final BakuActivityTrait<?> mBakuContext;

    public BakuDb(final RxDb db, final BakuActivityTrait bakuContext) {
        super(db);
        mBakuContext = bakuContext;
    }

    @Override
    public BakuTable rxTable(String name) {
        return new BakuTable(super.rxTable(name), mBakuContext);
    }
}
