// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import io.v.android.v23.V;
import io.v.baku.toolkit.blessings.BlessingsManagerBlessingsProvider;
import io.v.baku.toolkit.blessings.BlessingsProvider;
import io.v.baku.toolkit.blessings.BlessingsUtils;
import io.v.baku.toolkit.debug.DebugFragment;
import io.v.baku.toolkit.debug.DebugUtils;
import io.v.v23.Options;
import io.v.v23.context.VContext;
import io.v.v23.security.Blessings;
import io.v.v23.verror.VException;
import java8.util.function.BiFunction;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

/**
 * Android context mix-in incorporating common Vanadium utilities. These include:
 * <ul>
 * <li>Vanadium initialization during {@code onCreate}; context available via
 * {@code getVContext}</li>
 * <li>Blessings management, available via {@link BlessingsProvider#getRxBlessings()
 * getBlessingsProvider().getRxBlessings()}. Upon {@code subscribe}, blessings are refreshed from
 * the {@code BlessingsManager} or sought from the {@code BlessingsProvider} (by default, the
 * Vanadium {@link io.v.android.libs.security.BlessingsManager}).</li>
 * </ul>
 * Default activity extensions incorporating this mix-in are available:
 * <ul>
 * <li>{@link VActivity} (extends {@link Activity})</li>
 * <li>{@link VAppCompatActivity} (extends {@link android.support.v7.app.AppCompatActivity})</li>
 * </ul>
 * Since Java doesn't actually support multiple inheritance, clients requiring custom inheritance
 * hierarchies will need to wire in manually, like any of the examples above.
 */
@Accessors(prefix = "m")
@Slf4j
public class VAndroidContextMixin<T extends Context> implements VAndroidContextTrait<T> {
    public static SharedPreferences getVanadiumPreferences(final Context androidContext) {
        return androidContext.getSharedPreferences(VANADIUM_OPTIONS_SHARED_PREFS,
                Context.MODE_PRIVATE);
    }

    @Getter
    private final T mAndroidContext;
    @Getter
    private final BlessingsProvider mBlessingsProvider;
    @Getter
    private final ErrorReporter mErrorReporter;
    @Getter
    private final SharedPreferences mVanadiumPreferences;
    @Getter
    private final VContext mVContext;

    public Options getSavedOptions() {
        return VOptionPreferenceUtils.getOptionsFromPreferences(mVanadiumPreferences);
    }

    private VContext vinit() {
        try {
            return V.init(mAndroidContext, getSavedOptions());
        } catch (final RuntimeException e) {
            if (mVanadiumPreferences.getAll().isEmpty()) {
                throw e;
            } else {
                mErrorReporter.onError(R.string.err_vinit_options, e);
                // Don't actually clear/fix options here; leave that to the user
                return V.init(mAndroidContext);
            }
        }
    }

    public VAndroidContextMixin(final T androidContext, final BlessingsProvider blessingsProvider,
                                final ErrorReporter errorReporter) {
        this(androidContext, (x, y) -> blessingsProvider, errorReporter);
    }

    public VAndroidContextMixin(
            final T androidContext, final BiFunction<? super VContext, ? super T, BlessingsProvider>
            blessingsProviderFactory, final ErrorReporter errorReporter) {
        mAndroidContext = androidContext;
        mErrorReporter = errorReporter;

        mVanadiumPreferences = getVanadiumPreferences(mAndroidContext);
        mVContext = vinit();

        mBlessingsProvider = blessingsProviderFactory.apply(mVContext, mAndroidContext);

        //Any time our blessings change, we need to attach them to our principal.
        mBlessingsProvider.getPassiveRxBlessings().subscribe(this::processBlessings,
                t -> errorReporter.onError(R.string.err_blessings_misc, t));
    }

    @Override
    public void close() {
        mVContext.cancel();
    }

    protected void processBlessings(final Blessings blessings) {
        try {
            BlessingsUtils.assumeBlessings(mVContext, blessings);
        } catch (final VException e) {
            mErrorReporter.onError(R.string.err_blessings_assume, e);
        }
    }

    public static <T extends Activity> VAndroidContextMixin<T> withDefaults(
            final T activity, final Bundle savedInstanceState) {
        final FragmentManager mgr = activity.getFragmentManager();
        final ErrorReporterFragment errorReporter;

        if (savedInstanceState == null) {
            errorReporter = new ErrorReporterFragment();

            final FragmentTransaction t = mgr.beginTransaction()
                    .add(errorReporter, ErrorReporterFragment.TAG);

            if (DebugUtils.isApkDebug(activity)) {
                log.info("Debug menu enabled");
                t.add(new DebugFragment(), null);
            }
            t.commit();
        } else {
            errorReporter = ErrorReporterFragment.find(mgr);
        }
        return new VAndroidContextMixin<>(activity, BlessingsManagerBlessingsProvider::new,
                errorReporter);
    }
}
