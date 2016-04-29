// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android;

import android.content.Context;

import io.v.android.error.ErrorReporter;
import io.v.v23.context.VContext;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SimpleVAndroidContext<T extends Context> extends AbstractVAndroidContext<T> {
    private final T mAndroidContext;
    private final VContext mVContext;
    private final ErrorReporter mErrorReporter;
}
