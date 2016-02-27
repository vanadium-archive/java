// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

/**
 * This package is the entry point into the Baku Toolkit. The easiest way for an application to take
 * advantage of Baku is for its activities with distributed state to inherit from
 * {@link io.v.baku.toolkit.BakuActivity} (or {@link io.v.baku.toolkit.BakuAppCompatActivity}; see
 * [Mix-ins](#mixins) for other usage options).
 *
 * Baku components are built in layers bundling common sets of functionality. This allows
 * application developers the flexibility to selectively interact with APIs when they need to work
 * around our high-level abstractions which potentially don't meet their use cases.
 *
 * Sample code is available in the [baku projects repo]
 * (https://vanadium.googlesource.com/release.projects.baku).
 *
 * ## <a name="mixins"></a>Traits and Mix-ins
 *
 * * {@link io.v.baku.toolkit.BakuActivityTrait} / {@link io.v.baku.toolkit.BakuActivityMixin}
 * * {@link io.v.baku.toolkit.VAndroidContextTrait} / {@link io.v.baku.toolkit.VAndroidContextMixin}
 *
 * Android Activity classes can have different inheritance hierarchies depending on whether they are
 * edge-version {@link android.app.Activity}s or support-library
 * {@link android.support.v7.app.AppCompatActivity}s. Meanwhile the common code to support
 * {@link io.v.baku.toolkit.BakuActivity}s and {@link io.v.baku.toolkit.VActivity}s is most easily
 * used if included as superclass methods of the application Activity. Moreover, it really is an
 * "is-a" relationship. As such, multiple inheritance would be the natural way to arrange these
 * classes, but Java does not allow this.
 *
 * To still offer the same ease of use to client applications, we approximate [Scala-style mix-ins]
 * (http://docs.scala-lang.org/tutorials/tour/mixin-class-composition.html) by using "trait"
 * interfaces, "mix-in" classes, and tying them to Activity inheritance hierarchies by using Lombok
 * [`@Delegate`](https://projectlombok.org/features/experimental/Delegate.html) annotations. The
 * trait interfaces define the method signatures expected of `BakuActivity`s and `VActivity`s while
 * the mix-in classes define their implementations and associated private state. These mix-in
 * classes are included as `@Delegate`-annotated member fields of the `BakuActivity` and `VActivity`
 * classes, giving those classes implementations of {@link io.v.baku.toolkit.BakuActivityTrait} and
 * {@link io.v.baku.toolkit.VAndroidContextTrait} methods and allowing those classes to `implements`
 * those interfaces.
 *
 * Ideally `BakuActivityTrait`/`Mixin` would `extends VAndroidContextTrait`/`Mixin` in a secondary
 * inheritance ancestry, but the `@Delegate` annotation is not powerful enough to support nested
 * delegates, and that relation is instead expressed through pure composition, via a
 * {@link io.v.baku.toolkit.BakuActivityTrait#getVAndroidContextTrait() getVAndroidContextTrait()}
 * method.
 *
 * Applications can also wire these mix-ins into custom inheritance hierarchies by following the
 * example of [`BakuActivity`](https://goo.gl/e2Bkc2) and/or [`VActivity`](https://goo.gl/obS1qj).
 */
package io.v.baku.toolkit;