// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.moments.model;

import android.graphics.Bitmap;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import io.v.moments.ifc.Moment;
import io.v.moments.ifc.MomentFactory;
import io.v.moments.v23.ifc.AdCampaign;
import io.v.v23.context.VContext;
import io.v.v23.discovery.Attachments;
import io.v.v23.discovery.Attributes;
import io.v.v23.rpc.ServerCall;
import io.v.v23.security.BlessingPattern;

/**
 * Makes objects that support the advertisement of a Moment.
 */
public class MomentAdCampaign implements AdCampaign {
    /**
     * Required type/interface name, probably a URL into a web-based ontology.
     * Necessary for querying.
     */
    public static final String INTERFACE_NAME = "v.io/x/ref.Moments";
    /**
     * To limit scans to see only this service.
     */
    public static final String QUERY = "v.InterfaceName=\"" + INTERFACE_NAME + "\"";
    /**
     * Used for public advertisements (no limits on who can see them).
     */
    public static final List<BlessingPattern> NO_PATTERNS = new ArrayList<>();

    private final Moment mMoment;
    private final MomentFactory mFactory;

    public MomentAdCampaign(Moment moment, MomentFactory factory) {
        if (moment == null) {
            throw new IllegalArgumentException("Null moment");
        }
        if (factory == null) {
            throw new IllegalArgumentException("Null factory");
        }
        mMoment = moment;
        mFactory = factory;
    }

    @Override
    public String getInstanceId() {
        return mMoment.getId().toString();
    }

    @Override
    public String getInstanceName() {
        return mMoment.toString();
    }

    @Override
    public String getInterfaceName() {
        return INTERFACE_NAME;
    }

    @Override
    public Attributes getAttributes() {
        return mFactory.toAttributes(mMoment);
    }

    /**
     * No attachments (empty list).
     */
    @Override
    public Attachments getAttachments() {
        return new Attachments();
    }

    /**
     * Empty string means make no attempt to mount a server in a mount table.
     */
    @Override
    public String getMountName() {
        return "";
    }

    @Override
    public Object makeService() {
        return new MomentServer();
    }

    /**
     * A set of blessing patterns for whom this advertisement is meant; any
     * entity not matching a pattern here won't see the advertisement.
     */
    @Override
    public List<BlessingPattern> getVisibility() {
        return NO_PATTERNS;
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
