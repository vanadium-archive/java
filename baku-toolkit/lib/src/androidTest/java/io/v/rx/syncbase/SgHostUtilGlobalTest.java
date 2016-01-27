// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.rx.syncbase;

import io.v.baku.toolkit.VAndroidTestCase;
import io.v.baku.toolkit.blessings.BlessingsUtils;
import lombok.experimental.Delegate;

public class SgHostUtilGlobalTest extends VAndroidTestCase {

    @Delegate
    private SgHostUtilTestCases mTestCases;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        BlessingsUtils.addGlobalBlessingRoots(getVContext());
        mTestCases = new SgHostUtilTestCases(getContext(), getVContext());
    }
}
