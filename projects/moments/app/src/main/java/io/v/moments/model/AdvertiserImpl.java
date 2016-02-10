// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.moments.model;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import io.v.moments.ifc.Advertiser;
import io.v.moments.ifc.Moment;
import io.v.moments.ifc.Moment.Kind;
import io.v.moments.ifc.Moment.Style;
import io.v.moments.lib.V23Manager;
import io.v.v23.context.VContext;
import io.v.v23.discovery.Attachments;
import io.v.v23.discovery.Attributes;
import io.v.v23.discovery.Service;
import io.v.v23.naming.Endpoint;
import io.v.v23.rpc.ServerCall;
import io.v.v23.verror.VException;

/**
 * Handles the advertising of a moment.
 *
 * Main role of class is to manage the advertising context and moment service
 * lifetimes.  This code too complex to leak into the UX code, and too moment
 * specific to be part of v23Manager. This class should have no knowledge of UX,
 * and UX should have no access to VContext, because said context is under
 * active development, and it's too complex to cover possible interactions with
 * tests.  This approaches hides it all behind the simple Advertiser interface.
 */
public class AdvertiserImpl implements Advertiser {
    static final String NO_MOUNT_NAME = "";
    private static final String TAG = "AdvertiserImpl";

    private final V23Manager mV23Manager;
    private final Moment mMoment;

    private VContext mAdvCtx;
    private VContext mServerCtx;

    public AdvertiserImpl(V23Manager v23Manager, Moment moment) {
        if (v23Manager == null) {
            throw new IllegalArgumentException("Null v23Manager");
        }
        if (moment == null) {
            throw new IllegalArgumentException("Null moment");
        }
        mV23Manager = v23Manager;
        mMoment = moment;
    }

    @Override
    public String toString() {
        return mMoment.getCaption();
    }

    @Override
    public void start(
            FutureCallback<Void> startupCallback,
            FutureCallback<Void> completionCallback) {
        Log.d(TAG, "Entering start.");
        if (isAdvertising()) {
            startupCallback.onFailure(
                    new IllegalStateException("Already advertising."));
            return;
        }
        try {
            mServerCtx = mV23Manager.makeServerContext(
                    NO_MOUNT_NAME, new MomentServer());
        } catch (VException e) {
            mServerCtx = null;
            startupCallback.onFailure(
                    new IllegalStateException("Unable to start service.", e));
            return;
        }
        mV23Manager.advertise(
                makeAdvertisement(
                        mMoment.makeAttributes(), makeServerAddressList()),
                Config.Discovery.DURATION,
                makeWrapped(startupCallback), completionCallback);
        Log.d(TAG, "Exiting start.");
    }

    @Override
    public boolean isAdvertising() {
        return mAdvCtx != null && !mAdvCtx.isCanceled();
    }

    @NonNull
    private List<String> makeServerAddressList() {
        List<String> addresses = new ArrayList<>();
        Endpoint[] points = mV23Manager.getServer(
                mServerCtx).getStatus().getEndpoints();
        for (Endpoint point : points) {
            addresses.add(point.toString());
        }
        return addresses;
    }

    /**
     * A service must be started before advertising begins, which creates a
     * cleanup problem should advertising fail to start. This callback accepts
     * the advertising context on startup success, and cancels the already
     * started service on failure. This wrapper necessary to cleanly kill the
     * service should advertising fail to start.  This callback feeds info to
     * the startup callback, which presumably has some effect on UX.
     */
    private FutureCallback<VContext> makeWrapped(
            final FutureCallback<Void> startup) {
        return new FutureCallback<VContext>() {
            @Override
            public void onSuccess(VContext context) {
                mAdvCtx = context;
                startup.onSuccess(null);
            }

            @Override
            public void onFailure(final Throwable t) {
                mAdvCtx = null;
                cancelService();
                startup.onFailure(t);
            }
        };
    }

    @Override
    public void stop() {
        if (!isAdvertising()) {
            throw new IllegalStateException("Not advertising.");
        }
        Log.d(TAG, "Entering stop");
        if (mAdvCtx != null) {
            Log.d(TAG, "Cancelling advertising.");
            if (!mAdvCtx.isCanceled()) {
                mAdvCtx.cancel();
            }
            mAdvCtx = null;
        }
        cancelService();
        Log.d(TAG, "Exiting stop");
    }

    private void cancelService() {
        if (mServerCtx != null) {
            Log.d(TAG, "Cancelling service.");
            if (!mServerCtx.isCanceled()) {
                mServerCtx.cancel();
            }
            mServerCtx = null;
        }
    }

    /**
     * Makes an instance of 'Service', which is actually a service description,
     * i.e. an advertisement.
     */
    private Service makeAdvertisement(Attributes attrs,
                                      List<String> addresses) {
        return new Service(
                mMoment.getId().toString(),
                mMoment.toString(),
                Config.Discovery.INTERFACE_NAME,
                attrs,
                addresses,
                new Attachments());
    }

    /**
     * Serves moment data over RPC.
     */
    class MomentServer implements MomentIfcServer {
        private static final String TAG = "MomentServer";
        private byte[] mRawBytes = null;  // lazy init
        private byte[] mThumbBytes = null;  // lazy init

        public ListenableFuture<MomentWireData> getBasics(
                VContext ctx, ServerCall call) {
            MomentWireData data = new MomentWireData();
            data.setAuthor(mMoment.getAuthor());
            data.setCaption(mMoment.getCaption());
            data.setCreationTime(mMoment.getCreationTime().getMillis());
            return Futures.immediateFuture(data);
        }

        private byte[] makeBytes(Bitmap bitmap) {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 60, stream);
            return stream.toByteArray();
        }

        private synchronized byte[] getFullBytes() {
            if (mRawBytes == null) {
                mRawBytes = makeBytes(mMoment.getPhoto(Kind.LOCAL, Style.FULL));
            }
            return mRawBytes;
        }

        private synchronized byte[] getThumbBytes() {
            if (mThumbBytes == null) {
                mThumbBytes = makeBytes(mMoment.getPhoto(Kind.LOCAL, Style.THUMB));
            }
            return mThumbBytes;
        }

        public ListenableFuture<byte[]> getThumbImage(
                VContext ctx, ServerCall call) {
            return Futures.immediateFuture(getThumbBytes());
        }

        public ListenableFuture<byte[]> getFullImage(VContext ctx, ServerCall call) {
            return Futures.immediateFuture(getFullBytes());
        }
    }

}
