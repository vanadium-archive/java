// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit.syncbase;

import io.v.baku.toolkit.BakuActivityTrait;
import io.v.rx.syncbase.RxAndroidSyncbase;
import lombok.Getter;
import lombok.experimental.Accessors;

@Accessors(prefix = "m")
@Getter
public class BakuSyncbase extends RxAndroidSyncbase {
    private final BakuActivityTrait<?> mBakuContext;

    public BakuSyncbase(final BakuActivityTrait bakuContext) {
        super(bakuContext.getVAndroidContextTrait());
        mBakuContext = bakuContext;
    }

    @Override
    public BakuApp rxApp(String name) {
        return new BakuApp(super.rxApp(name), mBakuContext);
    }
}
