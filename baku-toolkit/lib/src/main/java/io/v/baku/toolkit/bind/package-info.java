// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

/**
 * These classes provide bindings between Android widgets and Syncbase data. Most functionality is
 * most easily accessed through access vectors in {@link io.v.baku.toolkit} rather than
 * instantiating directly from this package.
 *
 * For the reasons outlined in {@link io.v.rx.syncbase}, Vanadium state distribution with Syncbase
 * would ideally be done with pure FRP [MVVM]
 * (https://en.wikipedia.org/wiki/Model%E2%80%93view%E2%80%93viewmodel), with distributed state
 * elements modeled in Syncbase.
 *
 * ![MVVM illustration](doc-files/mvvm.png)
 *
 * **Fig: Ideal MVVM for distributed apps**
 *
 * However, while this is easily accomplished with [Flutter](https://flutter.io/), the Baku Toolkit
 * would need to implement an Android viewmodel layer to achieve the same for Android Java, which
 * starts to amount to reimplementing Flutter for Java. Instead, the Baku Android Toolkit offers
 * data bindings that allow Syncbase to drive more conventional Android Java UI widgets, and to
 * allow those widgets to update distributed state in Syncbase.
 *
 * ![Data bindings illustration](doc-files/bindings.png)
 *
 * **Fig: Baku data bindings without viewmodel**
 *
 * Even though these data bindings are not true MVVM, app developers are encouraged to treat them
 * declaratively, and to make use of pure functional transformations wherever possible to simplify
 * their code. Imperative code is however still useful for Android initialization, implementing
 * reactive widget update logic, and writing to Syncbase.
 *
 * Data bindings are offered to client applications via builders. Many facets of bindings are
 * derived from their usage context. For example, `bindTo(...)` methods perform type introspection
 * to construct appropriate binding types for the widget being bound (and possibly the row type
 * being bound to). In the future, we may add a plug-in to preprocess Android layout markup similar
 * to the [Android Data Binding Library]
 * (http://developer.android.com/tools/data-binding/guide.html).
 *
 * At present, for simplicity, each data binding that reads from Syncbase has its own Syncbase watch
 * stream. If this ends up wasting resources and degrading performance, we can optimize to minimize
 * the number of watch streams and broadcast filtered streams to each data binding.
 *
 * Offering data bindings rather than pure functional MVVM transforms does introduce some
 * coordination concerns between read and write bindings. Strategies for dealing with coordination
 * are included in the toolkit.
 */
package io.v.baku.toolkit.bind;