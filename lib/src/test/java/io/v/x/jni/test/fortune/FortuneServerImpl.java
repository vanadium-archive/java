// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.x.jni.test.fortune;

import com.google.common.collect.ImmutableList;
import io.v.v23.OutputChannel;
import io.v.v23.context.VContext;
import io.v.v23.naming.GlobError;
import io.v.v23.naming.GlobReply;
import io.v.v23.naming.MountEntry;
import io.v.v23.naming.MountedServer;
import io.v.v23.rpc.Globber;
import io.v.v23.rpc.ServerCall;
import io.v.v23.vdl.TypedStream;
import io.v.v23.vdl.VdlUint32;
import io.v.v23.verror.VException;

import java.io.EOFException;
import java.util.concurrent.CountDownLatch;

public class FortuneServerImpl implements FortuneServer, Globber {
    private static final ComplexErrorParam COMPLEX_PARAM = new ComplexErrorParam(
            "StrVal",
            11,
            ImmutableList.<VdlUint32>of(new VdlUint32(22), new VdlUint32(33)));

    public static final ComplexException COMPLEX_ERROR = new ComplexException(
            "en", "test", "test", COMPLEX_PARAM, "secondParam", 3);
    private final CountDownLatch latch;

    private String lastAddedFortune;

    public FortuneServerImpl() {
        this(null);
    }

    /**
     * If not {@code null}, the {@link FortuneServerImpl#get} method will block until the
     * latch is counted down. This allows for testing asynchronous RPCs.
     */
    public FortuneServerImpl(CountDownLatch latch) {
        this.latch = latch;
    }

    @Override
    public String get(VContext context, ServerCall call) throws VException {
        if (latch != null) {
            try {
                latch.await();
            } catch (InterruptedException e) {
                throw new VException(e.getMessage());
            }
        }
        if (lastAddedFortune == null) {
            throw new NoFortunesException(context);
        }
        return lastAddedFortune;
    }

    @Override
    public void add(VContext context, ServerCall call, String fortune) throws VException {
        lastAddedFortune = fortune;
    }

    @Override
    public int streamingGet(VContext context, ServerCall call, TypedStream<String, Boolean> stream)
            throws VException {
        int numSent = 0;
        while (true) {
            try {
                stream.recv();
            } catch (VException e) {
                throw new VException(
                        "Server couldn't receive a boolean item: " + e.getMessage());
            } catch (EOFException e) {
                break;
            }
            try {
                stream.send(get(context, call));
                } catch (VException e) {
                    throw new VException(
                        "Server couldn't send a string item: " + e.getMessage());
            }
            ++numSent;
        }
        return numSent;
    }

    @Override
    public MultipleGetOut multipleGet(VContext context, ServerCall call) throws VException {
        if (lastAddedFortune == null) {
            throw new NoFortunesException(context);
        }
        MultipleGetOut ret = new MultipleGetOut();
        ret.fortune = lastAddedFortune;
        ret.another = lastAddedFortune;
        return ret;
    }

    @Override
    public void getComplexError(VContext context, ServerCall call) throws VException {
        throw COMPLEX_ERROR;
    }

    @Override
    public void testServerCall(VContext context, ServerCall call) throws VException {
        if (call == null) {
            throw new VException("ServerCall is null");
        }
        if (call.suffix() == null) {
            throw new VException("Suffix is null");
        }
        if (call.localEndpoint() == null) {
            throw new VException("Local endpoint is null");
        }
        if (call.remoteEndpoint() == null) {
            throw new VException("Remote endpoint is null");
        }
        if (context == null) {
            throw new VException("Vanadium context is null");
        }
    }

    @Override
    public void noTags(VContext context, ServerCall call) throws VException {}

    @Override
    public void glob(ServerCall call, String pattern, OutputChannel<GlobReply> response)
            throws VException {
        GlobReply.Entry entry = new GlobReply.Entry(
                new MountEntry("helloworld", ImmutableList.<MountedServer>of(), false, false));
        response.writeValue(entry);
        GlobReply.Error error = new GlobReply.Error(
                new GlobError("Hello, world!", new VException("Some error")));
        response.writeValue(error);
        response.close();
    }
}
