// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit.bind;

import java.util.List;

import rx.Observable;

public interface ListDao<T, E> extends AutoCloseable {
    List<T> getList();
    Observable<E> getObservable();
    void close();
}
