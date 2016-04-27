// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.impl.google.services.gcm;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.common.util.concurrent.Uninterruptibles;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

import javax.annotation.Nullable;

import io.v.android.v23.V;
import io.v.v23.context.VContext;
import io.v.v23.rpc.Server;
import io.v.v23.verror.VException;

/**
 * Listens for GCM messages and wakes up services upon their receipt.
 */
public class GcmReceiveListenerService extends GcmListenerService {
    private static final String TAG = "GcmRecvListService";
    private static final String GCE_PROJECT_ID = "632758215260";

    private VContext mBaseContext;

    @Override
    public void onCreate() {
        mBaseContext = V.init(this);
    }

    @Override
    public void onDestroy() {
        mBaseContext.cancel();
    }

    @Override
    public void onMessageReceived(String from, Bundle data) {
        if (!from.equals(GCE_PROJECT_ID)) {
            Log.e(TAG, "Unknown GCM sender: " + from);
            return;
        }
        if (data == null) {
            return;
        }
        boolean wakeup = data.containsKey("vanadium_wakeup");
        if (!wakeup) {
            return;
        }
        String messageId = data.getString("message_id");
        if (messageId == null || messageId.isEmpty()) {
            Log.e(TAG, "Empty message_id field.");
            return;
        }
        String serviceName = data.getString("service_name");
        if (serviceName == null || serviceName.isEmpty()) {
            Log.e(TAG, "Empty service_name field.");
            return;
        }
        if (!serviceName.startsWith(getPackageName())) {
            return;
        }
        ComponentName service = new ComponentName(getPackageName(), serviceName);
        if (!GcmRegistrationService.isServiceRegistered(this, service)) {
            sendMsg(messageId, String.format("Service %s not registered for wakeup", serviceName));
            return;
        }
        wakeupService(service, messageId);
    }

    private void wakeupService(final ComponentName service, String msgId) {
        final SettableFuture<String> initDone = SettableFuture.create();
        Intent intent = new Intent();
        intent.setComponent(service);
        ServiceConnection conn = new ServiceConnection() {
            private boolean connected;

            @Override
            public void onServiceConnected(ComponentName componentName, IBinder binder) {
                connected = true;
                Futures.addCallback(onServiceReady(service, binder), new FutureCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        initDone.set("");
                    }
                    @Override
                    public void onFailure(Throwable t) {
                        initDone.set(t.toString());
                    }
                });
            }
            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                if (!connected) {
                    initDone.set("Couldn't connect to service " + componentName);
                }
            }
        };
        bindService(intent, conn, Context.BIND_AUTO_CREATE);
        String error;
        try {
            error = Uninterruptibles.getUninterruptibly(initDone);
        } catch (ExecutionException e) {
            error = e.toString();
        }
        // Start the service and unbind from it.
        startService(intent);
        unbindService(conn);
        sendMsg(msgId, error);
    }

    private void sendMsg(String msgId, String error) {
        Bundle b = new Bundle();
        b.putString("error", error);
        try {
            GoogleCloudMessaging.getInstance(this).send(
                    GCE_PROJECT_ID + "@gcm.googleapis.com", msgId, 1, b);
        } catch (IOException e) {
            Log.e(TAG, "Couldn't send GCM message.");
        }
    }

    private ListenableFuture<Void> onServiceReady(ComponentName service, IBinder binder) {
        if (binder == null) {
            Log.d(TAG, "Service %s returned null binder.");
            return Futures.immediateFuture(null);
        }
        Method getServiceMethod;
        try {
            getServiceMethod = binder.getClass().getDeclaredMethod("getServer");
        } catch (NoSuchMethodException e) {
            Log.d(TAG, String.format(
                    "Service %s binder doesn't have getServer() method: %s",
                    service, e.toString()));
            return Futures.immediateFuture(null);
        } catch (SecurityException e) {
            Log.e(TAG, String.format(
                    "Don't have permissions to find method information for service %s binder: %s",
                    service, e.toString()));
            return Futures.immediateFuture(null);
        }
        if (!getServiceMethod.getReturnType().isAssignableFrom(Server.class)) {
            Log.e(TAG, String.format(
                    "Service %s binder's getServer() method doesn't return a " +
                            "Vanadium Service object.", service));
            return Futures.immediateFuture(null);
        }
        Server server;
        try {
            server = (Server) getServiceMethod.invoke(binder);
        } catch (Exception e) {
            Log.e(TAG, String.format(
                    "Error invoking service %s binder's getService() method: %s",
                    service, e.toString()));
            return Futures.immediateFuture(null);
        }
        if (server == null) {
            Log.e(TAG, String.format(
                    "Service %s binder's getServer() method returned NULL Vanadium Server",
                    service));
            return Futures.immediateFuture(null);
        }
        return server.allPublished(mBaseContext);
    }
}
