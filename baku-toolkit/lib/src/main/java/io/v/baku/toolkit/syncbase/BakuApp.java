// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit.syncbase;

import io.v.baku.toolkit.BakuActivityTrait;
import io.v.rx.syncbase.RxApp;
import lombok.Getter;
import lombok.experimental.Accessors;

@Accessors(prefix = "m")
@Getter
public class BakuApp extends RxApp {
    private final BakuActivityTrait<?> mBakuContext;

    public BakuApp(final RxApp app, final BakuActivityTrait bakuContext) {
        super(app);
        mBakuContext = bakuContext;
    }

    @Override
    public BakuDb rxDb(String name) {
        return new BakuDb(super.rxDb(name), mBakuContext);
    }
}
