// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.moments.model;

import android.os.Handler;

import com.google.common.util.concurrent.ListenableFuture;

import org.joda.time.Duration;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import io.v.moments.v23.ifc.AdConverter;
import io.v.moments.ifc.Moment;
import io.v.moments.ifc.Moment.Kind;
import io.v.moments.ifc.Moment.Style;
import io.v.moments.ifc.MomentClientFactory;
import io.v.moments.ifc.MomentFactory;
import io.v.moments.lib.Id;
import io.v.moments.lib.ObservedList;
import io.v.moments.v23.ifc.V23Manager;
import io.v.v23.discovery.Service;

/**
 * Creates instances of Moment by making an RPC on a service.
 */
public class AdConverterMoment implements AdConverter<Moment> {
    private final V23Manager mV23Manager;
    private final ExecutorService mExecutor;
    private final Handler mHandler;
    private final MomentFactory mMomentFactory;
    private final Map<Id, Moment> mRemoteMomentCache;
    private final MomentClientFactory mClientFactory;
    private final boolean mDoFullSizeToo;
    private ObservedList<Moment> mMoments;

    // Package private for tests.
    AdConverterMoment(
            V23Manager v23Manager, MomentFactory factory,
            ExecutorService executor, Handler handler,
            Map<Id, Moment> remoteMomentCache,
            MomentClientFactory clientFactory, boolean doFullSizeToo) {
        mV23Manager = v23Manager;
        mMomentFactory = factory;
        mHandler = handler;
        mRemoteMomentCache = remoteMomentCache;
        mExecutor = executor;
        mClientFactory = clientFactory;
        mDoFullSizeToo = doFullSizeToo;
    }

    public AdConverterMoment(
            V23Manager v23Manager, MomentFactory factory,
            ExecutorService executor, Handler handler,
            Map<Id, Moment> remoteMomentCache) {
        this(v23Manager, factory, executor, handler, remoteMomentCache,
                new ClientFactoryImpl(), Config.DO_FULL_SIZE_TOO);
    }

    public void setList(ObservedList<Moment> moments) {
        mMoments = moments;
    }

    @Override
    public Moment make(Service descriptor) {
        if (mMoments == null) {
            throw new IllegalStateException("Must have a list to modify.");
        }
        Id id = Id.fromString(descriptor.getInstanceId());

        if (mRemoteMomentCache.containsKey(id)) {
            return mRemoteMomentCache.get(id);
        }
        final Moment moment = mMomentFactory.makeFromAttributes(
                id, nextOrdinal(), descriptor.getAttrs());
        mRemoteMomentCache.put(id, moment);

        List<String> addresses = descriptor.getAddrs();
        if (addresses.isEmpty()) {
            throw new IllegalStateException("No addresses.");
        }
        String name = "/" + addresses.get(0);
        final MomentIfcClient client = mClientFactory.makeClient(name);
        mExecutor.submit(new Runnable() {
            @Override
            public void run() {
                getThumbImage(client, moment);
                if (mDoFullSizeToo) {
                    getFullImage(client, moment);
                }
            }
        });
        return moment;
    }

    private int nextOrdinal() {
        int result = 0;
        for (Moment moment : mRemoteMomentCache.values()) {
            if (moment.getOrdinal() > result) {
                result = moment.getOrdinal();
            }
        }
        return result + 1;
    }

    private void getFullImage(final MomentIfcClient client, final Moment moment) {
        try {
            ListenableFuture<byte[]> data = client.getFullImage(
                    mV23Manager.contextWithTimeout(Deadline.FULL));
            moment.setPhoto(Kind.REMOTE, Style.FULL, data.get());
            signalChange(moment);
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    private void getThumbImage(final MomentIfcClient client, final Moment moment) {
        try {
            ListenableFuture<byte[]> data = client.getThumbImage(
                    mV23Manager.contextWithTimeout(Deadline.THUMB));
            moment.setPhoto(Kind.REMOTE, Style.THUMB, data.get());
            signalChange(moment);
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    private void signalChange(final Moment moment) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mMoments.changeById(moment.getId());
            }
        });
    }

    // Allows mocking of a static generated method.
    private static class ClientFactoryImpl implements MomentClientFactory {
        public MomentIfcClient makeClient(String name) {
            return MomentIfcClientFactory.getMomentIfcClient(name);
        }
    }

    static class Deadline {
        // Images can be slow to transfer when multiple images in flight, so
        // being generous with the deadline.  The impact of a timeout is just
        // a log message / stack trace, eventually giving something to
        // investigate if photos fail to appear.
        final static Duration FULL = Duration.standardSeconds(120);
        final static Duration THUMB = Duration.standardSeconds(30);
    }
}
