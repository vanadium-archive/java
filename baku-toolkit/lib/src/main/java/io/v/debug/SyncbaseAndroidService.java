// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.debug;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import org.apache.commons.io.FileUtils;
import org.joda.time.Duration;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import io.v.android.v23.V;
import io.v.baku.toolkit.blessings.BlessingsUtils;
import io.v.impl.google.services.syncbase.SyncbaseServer;
import io.v.rx.VFn;
import io.v.v23.context.VContext;
import io.v.v23.rpc.Server;
import io.v.v23.verror.VException;
import lombok.Value;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import rx.Observable;
import rx.Subscription;
import rx.schedulers.Schedulers;
import rx.util.async.Async;

/**
 * Syncbase Android service in lieu of GMS Core Syncbase. Exposes Syncbase as a simplistic bound
 * service, relying on Vanadium RPC in lieu of IPC, with open permissions, returning the local
 * Vanadium name in {@link Binder#getObservable()}...{@link BindResult#getName()}.
 */
@Slf4j
public class SyncbaseAndroidService extends Service {
    public static final String
            EXTRA_CLEAN_START = "EXTRA_CLEAN_START",
            EXTRA_KEEP_ALIVE = "EXTRA_KEEP_ALIVE";

    public static final Duration DEFAULT_KEEP_ALIVE = Duration.standardMinutes(5);
    private static final Duration STOP_TIMEOUT = Duration.standardSeconds(5);

    @Accessors(prefix = "m")
    @Value
    public static class BindResult {
        Server mServer;
        String mName;
    }

    private VContext mVContext;
    private Observable<BindResult> mObservable;
    private Subscription mKill;
    private Duration mKeepAlive;

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
        // If we don't proxy, it seems we can't even mount.
        try {
            mVContext = V.withListenSpec(mVContext, V.getListenSpec(mVContext).withProxy("proxy"));
        } catch (final VException e) {
            log.warn("Unable to set up Vanadium proxy for Syncbase", e);
        }
    }

    @Override
    public void onDestroy() {
        try {
            mObservable.doOnNext(VFn.unchecked(b -> {
                log.info("Stopping Syncbase");
                b.mServer.stop();
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
        mKill = Observable.timer(mKeepAlive.getMillis(), TimeUnit.MILLISECONDS)
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

        mKeepAlive = (Duration)intent.getSerializableExtra(EXTRA_KEEP_ALIVE);
        if (mKeepAlive == null) {
            mKeepAlive = DEFAULT_KEEP_ALIVE;
        }

        if (intent.getBooleanExtra(EXTRA_CLEAN_START, false)) {
            log.info("Clearing Syncbase data per intent");
            try {
                FileUtils.deleteDirectory(storageRoot);
            } catch (final IOException e) {
                log.error("Could not clear Syncbase data", e);
            }
        }

        storageRoot.mkdirs();

        log.info("Starting Syncbase");
        final VContext sbCtx = SyncbaseServer.withNewServer(mVContext, new SyncbaseServer.Params()
                .withPermissions(BlessingsUtils.OPEN_DATA_PERMS)
                .withStorageRootDir(storageRoot.getAbsolutePath()));
        final Server server = V.getServer(sbCtx);
        return new BindResult(server, "/" + server.getStatus().getEndpoints()[0]);
    }
}
