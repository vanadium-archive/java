// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

/**
 * This package is the entry point into the Baku Toolkit. The easiest way for an application to take
 * advantage of Baku is for its activities with distributed state to inherit from
 * {@link io.v.baku.toolkit.BakuActivity} (or {@link io.v.baku.toolkit.BakuAppCompatActivity}; see
 * {@link io.v.baku.toolkit.BakuActivityMixin} for custom inheritance trees). Then, for any UI
 * widget that should have distributed state, the client application should build data bindings by
 * chaining methods from a {@link io.v.baku.toolkit.BakuActivityTrait#binder() binder()} call,
 * binding shared data fields to UI widget properties. For <a href="https://goo.gl/P0Ag9a"
 * target="_blank">example</a>, the following binds a data key named {@code "text"} to the text of a
 * {@link android.widget.TextView} with ID {@code textView}:
 * <pre><code>
 * &#64;Override
 * protected void onCreate(final Bundle savedInstanceState) {
 *     super.onCreate(savedInstanceState);
 *     setContentView(R.layout.activity_layout);
 *
 *     {@link io.v.baku.toolkit.BakuActivityMixin#binder() binder}().{@link
 *     io.v.baku.toolkit.bind.SyncbaseBinding.Builder#key(java.lang.String) key}("text")
 *             .{@link io.v.baku.toolkit.bind.SyncbaseBinding.Builder#bindTo(int)
 *             bindTo}(R.id.textView);
 *     }
 * }
 * </code></pre>
 * Collection bindings (from vector data to list/recycler views) are similarly exposed through a
 * {@link io.v.baku.toolkit.BakuActivityTrait#collectionBinder() collectionBinder()} builder. Writes
 * can be performed directly via {@link io.v.baku.toolkit.syncbase.BakuTable#put(java.lang.String,
 * java.lang.Object) getSyncbaseTable().put(key, value)}. More information about data bindings is
 * available in the {@link io.v.baku.toolkit.bind} package documentation.
 * <p>
 * The Baku Toolkit creates a Syncbase table to use by default for data binding, and creates and
 * manages a default {@linkplain io.v.rx.syncbase.UserCloudSyncgroup global user-level cloud
 * syncgroup} to sync distributed data across all instances of the application belonging to a user.
 * <p>
 * Baku components are built in layers bundling common sets of functionality. This allows
 * application developers the flexibility to selectively interact with APIs when they need to work
 * around our high-level abstractions which potentially don't meet their use cases.
 * <p>
 * Sample code is available in the
 * <a href="https://vanadium.googlesource.com/release.projects.baku/" target="_blank">baku projects
 * repo</a>.
 */
package io.v.baku.toolkit;