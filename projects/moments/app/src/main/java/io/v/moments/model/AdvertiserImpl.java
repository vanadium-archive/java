// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.moments.model;

import android.graphics.Bitmap;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import io.v.moments.ifc.Advertiser;
import io.v.moments.ifc.Moment;
import io.v.moments.ifc.Moment.Kind;
import io.v.moments.ifc.Moment.Style;
import io.v.moments.ifc.MomentFactory;
import io.v.moments.lib.V23Manager;
import io.v.v23.context.CancelableVContext;
import io.v.v23.context.VContext;
import io.v.v23.discovery.Attributes;
import io.v.v23.discovery.Service;
import io.v.v23.discovery.VDiscovery;
import io.v.v23.naming.Endpoint;
import io.v.v23.rpc.Server;
import io.v.v23.rpc.ServerCall;
import io.v.v23.security.BlessingPattern;
import io.v.v23.verror.VException;

/**
 * Handles the advertising of a moment.
 */
public class AdvertiserImpl implements Advertiser {
    private static final String TAG = "AdvertiserImpl";
    private static final List<BlessingPattern> NO_PATTERNS = new ArrayList<>();
    static final String NO_MOUNT_NAME = "";
    private final V23Manager mV23Manager;
    private final Moment mMoment;

    private CancelableVContext mAdvCtx;
    private Server mServer;

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
    public boolean isAdvertising() {
        return mAdvCtx != null;
    }

    @Override
    public String toString() {
        return mMoment.getCaption();
    }

    @Override
    public void advertiseStart() {
        if (isAdvertising()) {
            throw new IllegalStateException("Already advertising.");
        }
        try {
            mServer = mV23Manager.makeServer(NO_MOUNT_NAME, new MomentServer());
        } catch (VException e) {
            throw new IllegalStateException("Unable to start service.", e);
        }
        List<String> addresses = new ArrayList<>();
        Endpoint[] points = mServer.getStatus().getEndpoints();
        for (Endpoint point : points) {
            addresses.add(point.toString());
        }
        Attributes attrs = mMoment.makeAttributes();
        mAdvCtx = mV23Manager.advertise(
                makeAdvertisement(attrs, addresses),
                NO_PATTERNS,
                new VDiscovery.AdvertiseDoneCallback() {
                    @Override
                    public void done() {
                        // Nothing in particular to do here.
                    }
                });
    }

    @Override
    public void advertiseStop() {
        if (!isAdvertising()) {
            throw new IllegalStateException("Not advertising.");
        }
        mAdvCtx.cancel();
        try {
            mServer.stop();
        } catch (VException e) {
            throw new IllegalStateException("Unable to stop service.", e);
        }
        mAdvCtx = null;
        mServer = null;
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
                Config.INTERFACE_NAME,
                attrs,
                addresses);
    }

    /**
     * Serves moment data over RPC.
     */
    class MomentServer implements MomentIfcServer {
        private static final String TAG = "MomentServer";
        private byte[] mRawBytes = null;  // lazy init
        private byte[] mThumbBytes = null;  // lazy init

        public MomentWireData getBasics(VContext ctx, ServerCall call)
                throws VException {
            MomentWireData data = new MomentWireData();
            data.setAuthor(mMoment.getAuthor());
            data.setCaption(mMoment.getCaption());
            data.setCreationTime(mMoment.getCreationTime().getMillis());
            return data;
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

        public byte[] getThumbImage(VContext ctx, ServerCall call)
                throws VException {
            return getThumbBytes();
        }

        public byte[] getFullImage(VContext ctx, ServerCall call)
                throws VException {
            return getFullBytes();
        }
    }

}