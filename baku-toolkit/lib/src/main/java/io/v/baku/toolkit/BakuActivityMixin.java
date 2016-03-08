// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit;

import android.app.Activity;
import android.os.Bundle;

import io.v.baku.toolkit.bind.BindingBuilder;
import io.v.baku.toolkit.syncbase.BakuDb;
import io.v.baku.toolkit.syncbase.BakuSyncbase;
import io.v.baku.toolkit.syncbase.BakuTable;
import io.v.rx.syncbase.UserCloudSyncgroup;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;

/**
 * Backing [mix-in](package-summary.html#mixins) for {@link BakuActivityTrait}. By default, shared
 * state is stored in Syncbase under _app.package.name_/db/ui.
 *
 * Default `Activity` subclasses incorporating this mix-in are available:
 *
 * * {@link BakuActivity} (`extends {@link Activity}`)
 * * {@link BakuAppCompatActivity} (`extends {@link android.support.v7.app.AppCompatActivity}`)
 *
 * Since Java doesn't actually support multiple inheritance, clients requiring custom inheritance
 * hierarchies will need to wire in manually, like any of the examples above. Alternatively, this
 * class may be used via pure composition:
 *
 * ```java
 * public class SampleCompositionActivity extends Activity {
 *     private {@link BakuActivityTrait}<SampleCompositionActivity> mBaku;
 *
 *     {@literal @}Override
 *     protected void onCreate(final Bundle savedInstanceState) {
 *         super.onCreate(savedInstanceState);
 *         setContentView(R.layout.my_activity_layout);
 *
 *         mBaku = new {@link #BakuActivityMixin(Activity, Bundle)
 *             BakuActivityMixin}<>(this, savedInstanceState);
 *
 *         // Example binding between "myDataRow" in Syncbase and myTextView in my_activity_layout.
 *         mBaku.{@link #dataBinder() dataBinder}()
 *              .{@link BindingBuilder#onKey(java.lang.String) onKey}("myDataRow")
 *              .{@link io.v.baku.toolkit.bind.ScalarBindingBuilder#bindTo(int)
 *                  bindTo}(R.id.myTextView);
 *     }
 *
 *     {@literal @}Override
 *     protected void onDestroy() {
 *         mBaku.{@link BakuActivityMixin#close() close}();
 *         super.onDestroy();
 *     }
 * }
 * ```
 *
 * @see io.v.baku.toolkit
 */
@Accessors(prefix = "m")
@Slf4j
public class BakuActivityMixin<T extends Activity> implements BakuActivityTrait<T> {
    @Getter
    private final VAndroidContextTrait<T> mVAndroidContextTrait;

    @Getter
    private final BakuSyncbase mSyncbase;
    @Getter
    private final BakuDb mSyncbaseDb;
    @Getter
    private final BakuTable mSyncbaseTable;
    @Getter
    private final CompositeSubscription mSubscriptions;

    public BakuActivityMixin(final VAndroidContextTrait<T> vAndroidContextTrait) {
        mVAndroidContextTrait = vAndroidContextTrait;

        mSubscriptions = new CompositeSubscription();
        mSyncbase = new BakuSyncbase(this);

        final String app = getSyncbaseAppName(),
                db = getSyncbaseDbName(),
                t = getSyncbaseTableName();
        log.info("Mapping Syncbase path: {}/{}/{}", app, db, t);
        mSyncbaseDb = mSyncbase.rxApp(app).rxDb(db);
        mSyncbaseTable = mSyncbaseDb.rxTable(t);

        joinInitialSyncGroup();
    }

    /**
     * Convenience constructor for compositional integration. For example usage,
     * see the {@linkplain BakuActivityMixin class docs}.
     */
    public BakuActivityMixin(final T context, final Bundle savedInstanceState) {
        this(VAndroidContextMixin.withDefaults(context, savedInstanceState));
        // We have to manage this VAndroidContextTrait since we created it.
        mSubscriptions.add(Subscriptions.create(mVAndroidContextTrait::close));
    }

    @Override
    public void close() {
        mSubscriptions.unsubscribe();
        mSyncbase.close();
    }

    protected String getSyncbaseAppName() {
        return mVAndroidContextTrait.getAndroidContext().getPackageName();
    }

    protected String getSyncbaseDbName() {
        return "db";
    }

    @Override
    public String getSyncbaseTableName() {
        return "ui";
    }

    protected void joinInitialSyncGroup() {
        UserCloudSyncgroup.forActivity(this).join();
    }

    @Override
    public void onSyncError(final Throwable t) {
        ErrorReporters.getDefaultSyncErrorReporter(mVAndroidContextTrait);
    }

    @Override
    public BindingBuilder dataBinder() {
        return new BindingBuilder().activity(this);
    }
}
