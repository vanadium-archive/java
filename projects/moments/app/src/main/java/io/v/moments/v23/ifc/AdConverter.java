// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.moments.v23.ifc;

import io.v.v23.discovery.Advertisement;

/**
 * Implementations of this interface construct an instance of T from the
 * attributes in the advertisement, and/or by making RPCs to services associated
 * with or otherwise mentioned by the advertisement.
 *
 * TODO(jregan): This method should return ListenableFuture<T>.
 */
public interface AdConverter<T> {
    T make(Advertisement advertisement);
}
