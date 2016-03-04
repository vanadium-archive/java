// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit;

import android.app.Activity;

import io.v.baku.toolkit.bind.BindingBuilder;
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
 * distributed storage system to UI widget properties. Writes can be performed directly via
 * {@link #getSyncbaseTable()}`.`{@link io.v.baku.toolkit.syncbase.BakuTable#put(java.lang.String,
 * java.lang.Object) put(key, value)}. For two-way scalar bindings, writes can also be performed by
 * modifying the widget directly. More information about data bindings is available in the
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
     * getSyncbaseTable().{@link BakuTable#put(String, Object) put}("myKey", myValue);
     * ```
     */
    BakuTable getSyncbaseTable();
    CompositeSubscription getSubscriptions();
    String getSyncbaseTableName();
    void onSyncError(Throwable t);

    /**
     * Exposes a default binding builder for this Activity. The returned builder may be freely
     * customized; a new builder is returned for each call.
     *
     * Example usage:
     *
     * ```java
     * binder().{@link BindingBuilder#onKey(java.lang.String) onKey}("myDataRow")
     *         .{@link io.v.baku.toolkit.bind.ScalarBindingBuilder#bindTo(int)
     *         bindTo}(R.id.myTextView);
     * ```
     */
    BindingBuilder binder();

    /**
     * Unsubscribes all data bindings associated with this activity and releases the local Syncbase
     * instance.
     *
     * @see BakuSyncbase#close()
     */
    void close();
}
