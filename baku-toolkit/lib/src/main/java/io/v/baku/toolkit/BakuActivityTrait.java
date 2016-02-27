// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit;

import android.app.Activity;

import io.v.baku.toolkit.bind.SyncbaseBinding;
import io.v.baku.toolkit.bind.CollectionBinding;
import io.v.baku.toolkit.syncbase.BakuDb;
import io.v.baku.toolkit.syncbase.BakuSyncbase;
import io.v.baku.toolkit.syncbase.BakuTable;
import rx.subscriptions.CompositeSubscription;

/**
 * [Trait](package-summary.html#mixins) for activities with distributed UI state. This trait is
 * implemented by {@link BakuActivityMixin}.
 *
 * For any UI widget that should have distributed state, the application should build data bindings
 * by chaining methods from a {@link #binder()} call, binding shared data fields in the Syncbase
 * distributed storage system to UI widget properties.
 *
 * Collection bindings (from vector data to list/recycler views) are similarly exposed through a
 * {@link #collectionBinder() collectionBinder()} builder. Writes can be performed directly via
 * {@link #getSyncbaseTable()}`.`{@link io.v.baku.toolkit.syncbase.BakuTable#put(java.lang.String,
 * java.lang.Object) put(key, value)}. More information about data bindings is available in the
 * {@link io.v.baku.toolkit.bind} package documentation.
 *
 * Implementations create a Syncbase table to use by default for data binding, and create and manage
 * a default {@linkplain io.v.rx.syncbase.UserCloudSyncgroup global user-level cloud syncgroup} to
 * sync distributed data across all instances of the application belonging to a user.
 */
public interface BakuActivityTrait<T extends Activity> extends AutoCloseable {
    VAndroidContextTrait<T> getVAndroidContextTrait();
    BakuSyncbase getSyncbase();
    BakuDb getSyncbaseDb();

    /**
     * Gets a wrapper for the Syncbase table, allowing for direct write operations. The wrapper
     * includes {@link #onSyncError(Throwable)} as a default error handler for any actions that are
     * not explicitly subscribed to, making write operations easier to use.
     *
     * Example usage:
     *
     * ```java
     * getSyncbaseTable().put("myKey", myValue);
     * ```
     */
    BakuTable getSyncbaseTable();
    CompositeSubscription getSubscriptions();
    String getSyncbaseTableName();
    void onSyncError(Throwable t);

    /**
     * Exposes a default scalar data binding builder for this Activity. The returned builder may be
     * freely customized; a new builder is returned for each call.
     *
     * Example usage:
     *
     * ```java
     * binder().{@link io.v.baku.toolkit.bind.SyncbaseBinding.Builder#key(java.lang.String)
     *     key}("myDataRow")
     *         .{@link io.v.baku.toolkit.bind.SyncbaseBinding.Builder#bindTo(int)
     *         bindTo}(R.id.myTextView);
     * ```
     */
    <U> SyncbaseBinding.Builder<U> binder();

    /**
     * Exposes a default collection data binding builder for this Activity. The returned builder may
     * be freely customized; a new builder is returned for each call.
     *
     * Example usage:
     *
     * ```java
     * collectionBinder()
     *         .{@link io.v.baku.toolkit.bind.CollectionBinding.Builder#onPrefix(String)
     *             onPrefix}("myListItems/")
     *         .{@link io.v.baku.toolkit.bind.PrefixBindingBuilder#type(Class) type}(String.class)
     *         .{@link io.v.baku.toolkit.bind.PrefixBindingBuilder#bindTo(int)
     *             bindTo}(R.id.myListView);
     * ```
     */
    CollectionBinding.Builder collectionBinder();

    /**
     * Unsubscribes all data bindings associated with this activity and releases the local Syncbase
     * instance.
     *
     * @see BakuSyncbase#close()
     */
    void close();
}
