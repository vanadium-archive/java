// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.util;

import io.v.v23.context.VContext;

/**
 * Vango allows arbitrary Vanadium Go code to be run in an android
 * environment.
 *
 * See https://github.com/vanadium/java/blob/master/projects/vango/README.md for instructions
 * on writing Go code and running the Android app.
 */
public class Vango {
  /* Interface used for the Go function to send data to be shown to the user back to Java */
  public interface OutputWriter {
    void write(String output);
  }

  private native void nativeGoContextCall(VContext context, String key, OutputWriter output);

  public void run(VContext context, String key, OutputWriter output) {
    nativeGoContextCall(context, key, output);
  }
}
