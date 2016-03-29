// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.debug;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import org.apache.commons.io.FileUtils;
import org.joda.time.Duration;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.concurrent.TimeUnit;

import io.v.android.v23.V;
import io.v.baku.toolkit.blessings.BlessingsUtils;
import io.v.impl.google.services.syncbase.SyncbaseServer;
import io.v.rx.VFn;
import io.v.v23.context.VContext;
import io.v.v23.rpc.Server;
import io.v.v23.verror.VException;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.experimental.Accessors;
import lombok.experimental.Wither;
import lombok.extern.slf4j.Slf4j;
import rx.Observable;
import rx.Subscription;
import rx.schedulers.Schedulers;
import rx.util.async.Async;

/**
 * Syncbase Android service in lieu of GMS Core Syncbase. Exposes Syncbase as a simplistic bound
 * service, relying on Vanadium RPC in lieu of IPC, with open permissions, returning the local
 * Vanadium name in {@link Binder#getObservable()}...{@link BindResult#getEndpoint()}.
 */
@Slf4j
public class SyncbaseAndroidService extends Service {
    public static final String EXTRA_OPTIONS = "EXTRA_OPTIONS";

    @RequiredArgsConstructor
    @Wither
    @Accessors(prefix = "") // override default 'm' prefix since these are public fields
    public static class Options implements Serializable {
        /**
         * Whether or not Syncbase data are cleared prior to service startup. Defaults to false.
         */
        public final boolean cleanStart;
        /**
         * Time to keep the Syncbase service alive after the last (local) client disconnects.
         * Defaults to 5 minutes.
         */
        public final Duration keepAlive;
        /**
         * Name to mount at. If `null`, the service is not initially mounted externally. Defaults to
         * `null`.
         */
        public final
        @Nullable
        String name;
        /**
         * Name of the Vanadium proxy to use. If `null`, no proxy is used. Defaults to `"proxy"`.
         */
        public final
        @Nullable
        String proxy;

        public Options() {
            this(false, Duration.standardMinutes(5), null, "proxy");
        }
    }

    private static final Duration STOP_TIMEOUT = Duration.standardSeconds(5);

    @Accessors(prefix = "m")
    @Value
    public static class BindResult {
        Server mServer;
        String mEndpoint;
    }

    private VContext mVContext;
    private Observable<BindResult> mObservable;
    private Subscription mKill;
    private Options mOptions;

    public class Binder extends android.os.Binder {
        private Binder() {
        }

        public Observable<BindResult> getObservable() {
            return mObservable;
        }
    }

    @Override
    public void onCreate() {
        mVContext = V.init(this);
    }

    @Override
    public void onDestroy() {
        try {
            mObservable.doOnNext(VFn.unchecked(b -> {
                log.info("Stopping Syncbase");
                mVContext.cancel();
            }))
                    .timeout(STOP_TIMEOUT.getMillis(), TimeUnit.MILLISECONDS)
                    .toBlocking()
                    .single();
            log.info("Syncbase is over");
            // TODO(rosswang): https://github.com/vanadium/issues/issues/809
            System.exit(0);
        } catch (final RuntimeException e) {
            log.error("Failed to shut down Syncbase", e);
            System.exit(1);
        }
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        ensureStarted(intent);
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(final Intent intent) {
        ensureStarted(intent);
        return new Binder();
    }

    @Override
    public boolean onUnbind(final Intent intent) {
        mKill = Observable.timer(mOptions.keepAlive.getMillis(), TimeUnit.MILLISECONDS)
                .subscribe(x -> stopSelf());
        return true;
    }

    @Override
    public void onRebind(final Intent intent) {
        mKill.unsubscribe();
    }

    private void ensureStarted(final Intent intent) {
        if (mObservable == null) {
            mObservable = Async.fromCallable(() -> startServer(intent), Schedulers.io())
                    .replay(1).autoConnect(0); //cache last result; connect immediately
        }
    }

    private BindResult startServer(final Intent intent) throws SyncbaseServer.StartException {
        final File storageRoot = new File(getFilesDir(), "syncbase");

        mOptions = (Options) intent.getSerializableExtra(EXTRA_OPTIONS);
        if (mOptions == null) {
            mOptions = new Options();
        }

        if (mOptions.cleanStart) {
            log.info("Clearing Syncbase data per intent");
            try {
                FileUtils.deleteDirectory(storageRoot);
            } catch (final IOException e) {
                log.error("Could not clear Syncbase data", e);
            }
        }

        VContext serverContext = mVContext;
        if (mOptions.proxy != null) {
            try {
                serverContext = V.withListenSpec(mVContext, V.getListenSpec(mVContext)
                        .withProxy(mOptions.proxy));
            } catch (final VException e) {
                log.warn("Unable to set up Vanadium proxy for Syncbase", e);
            }
        }

        storageRoot.mkdirs();

        log.info("Starting Syncbase");
        final VContext sbCtx = SyncbaseServer.withNewServer(serverContext,
                new SyncbaseServer.Params()
                        .withPermissions(BlessingsUtils.OPEN_DATA_PERMS)
                        .withStorageRootDir(storageRoot.getAbsolutePath())
                        .withName(mOptions.name)); // name is ignored if null
        final Server server = V.getServer(sbCtx);
        return new BindResult(server, "/" + server.getStatus().getEndpoints()[0]);
    }
}
