// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.util;

import java.lang.Runnable;

import io.v.v23.context.VContext;

/**
 * V23GoRunnable allows arbritrary Vanadium Go code to be run in an android
 * environment.
 * Users must edit V23GoRunnableFunc in
 * https://vanadium.googlesource.com/release.go.x.jni/+/master/impl/google/services/v23_go_runnable/jni.go
 * and rebuild android-lib.
 */
public class V23GoRunnable implements Runnable {
  private native void nativeGoContextCall(VContext context);

  private VContext context;

  public void run() {
    nativeGoContextCall(context);
  }

  public V23GoRunnable(VContext context) {
    this.context = context;
  }
}
