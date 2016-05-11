// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.util;

import io.v.v23.context.VContext;

/**
 * Vango allows arbritrary Vanadium Go code to be run in an android
 * environment.
 * Users must edit the vangoFuncs map in
 * https://vanadium.googlesource.com/release.go.x.jni/+/master/impl/google/services/vango/funcs.go
 * and rebuild android-lib.
 */
public class Vango {
  private native void nativeGoContextCall(VContext context, String string);

  private VContext context;

  public void run(VContext context, String string) {
    nativeGoContextCall(context, string);
  }

  public Vango() {
    this.context = context;
  }
}
