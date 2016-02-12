// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.moments.v23.ifc;

import io.v.v23.discovery.Service;

/**
 * The io.v.v23.discovery.Service isn't a service, it's the *description* of a
 * service used as a discovery advertisement.
 *
 * Implementations of this interface construct an instance of T from the
 * attributes in Service, and/or by making an RPC to the real underlying service
 * described by the advertisement to get data needed to make a T.
 */
public interface AdConverter<T> {
    T make(Service service);
}
