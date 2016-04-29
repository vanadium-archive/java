// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import io.v.android.error.ErrorReporter;
import io.v.android.error.ErrorReporterFragment;
import io.v.android.debug.DebugFragment;
import io.v.android.debug.Debug;
import io.v.v23.context.VContext;
import java8.util.function.BiFunction;
import lombok.experimental.UtilityClass;

@UtilityClass
public class VAndroidContexts {
    private static final String TAG = VAndroidContexts.class.getSimpleName();

    public static SharedPreferences getVanadiumPreferences(final Context androidContext) {
        return androidContext.getSharedPreferences(VAndroidContext.VANADIUM_OPTIONS_SHARED_PREFS,
                Context.MODE_PRIVATE);
    }

    /**
     * Creates a {@code ManagedVAndroidContext} with default settings. Default settings include
     * using {@link ErrorReporterFragment} as the {@link ErrorReporter}, and if the apk is built as
     * {@linkplain Debug#isApkDebug(Context) debug}, a {@linkplain DebugFragment debug menu} is
     * included.
     * <p>
     * The signature of this method is intended to be called from {@link Activity#onCreate(Bundle)}
     * or similar methods. If the activity is being restored with its fragments intact (i.e.
     * {@code savedInstanceState != null}), the existing fragments are reused.
     */
    public static <T extends Activity> ManagedVAndroidContext<T> withDefaults(
            final T activity, final Bundle savedInstanceState) {
        return withDefaults(activity, savedInstanceState, ManagedVAndroidContext::new);
    }

    /**
     * Creates a {@link VAndroidContext} with default settings, given a constructor function.
     *
     * @see #withDefaults(Activity, Bundle)
     */
    public static <T extends Activity, C extends VAndroidContext> C withDefaults(
            final T activity, final Bundle savedInstanceState,
            final BiFunction<T, ErrorReporter, C> constructor) {
        final FragmentManager mgr = activity.getFragmentManager();
        final ErrorReporterFragment errorReporter;
        final DebugFragment debug;

        if (savedInstanceState == null) {
            errorReporter = new ErrorReporterFragment();

            final FragmentTransaction t = mgr.beginTransaction()
                    .add(errorReporter, ErrorReporterFragment.FRAGMENT_TAG);

            if (Debug.isApkDebug(activity)) {
                Log.i(TAG, "Debug menu enabled");
                debug = new DebugFragment();
                t.add(debug, null);
            } else {
                debug = null;
            }
            t.commit();
        } else {
            errorReporter = ErrorReporterFragment.find(mgr);
            debug = DebugFragment.find(mgr);
        }
        final C vac = constructor.apply(activity, errorReporter);

        if (debug != null) {
            debug.setVAndroidContext(vac);
        }

        return vac;
    }

    public static <T extends Context> VAndroidContext<T> simple(
            final T context, final VContext vContext, final ErrorReporter errorReporter) {
        return new SimpleVAndroidContext<>(context, vContext, errorReporter);
    }
}
