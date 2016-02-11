// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.moments.model;

import android.graphics.Bitmap;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.ByteArrayOutputStream;
import java.util.List;

import io.v.moments.ifc.AdSupporter;
import io.v.moments.ifc.Moment;
import io.v.v23.context.VContext;
import io.v.v23.discovery.Attachments;
import io.v.v23.discovery.Service;
import io.v.v23.rpc.ServerCall;

/**
 * Makes objects that support the advertisement of a Moment.
 */
class MomentAdSupporter implements AdSupporter {
    private static final String NO_MOUNT_NAME = "";

    private final Moment mMoment;

    public MomentAdSupporter(Moment moment) {
        mMoment = moment;
    }

    public String getMountName() {
        return NO_MOUNT_NAME;
    }

    public Object makeServer() {
        return new MomentServer();
    }

    /**
     * Makes an instance of 'Service', which is actually a service description,
     * i.e. an advertisement.
     */
    public Service makeAdvertisement(List<String> addresses) {
        return new Service(
                mMoment.getId().toString(), /* instance Id */
                mMoment.toString(), /* instance name */
                Config.Discovery.INTERFACE_NAME, /* interface name */
                mMoment.makeAttributes(),
                addresses,
                new Attachments() /* no attachments */);
    }

    /**
     * Serves moment data over RPC.
     */
    private class MomentServer implements MomentIfcServer {
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
                mRawBytes = makeBytes(mMoment.getPhoto(Moment.Kind.LOCAL, Moment.Style.FULL));
            }
            return mRawBytes;
        }

        private synchronized byte[] getThumbBytes() {
            if (mThumbBytes == null) {
                mThumbBytes = makeBytes(mMoment.getPhoto(Moment.Kind.LOCAL, Moment.Style.THUMB));
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
