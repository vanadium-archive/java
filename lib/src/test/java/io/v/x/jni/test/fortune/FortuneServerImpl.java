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
import io.v.v23.vdl.Stream;
import io.v.v23.vdl.VdlUint32;
import io.v.v23.verror.VException;

import java.io.EOFException;

public class FortuneServerImpl implements FortuneServer, Globber {
    private static final ComplexErrorParam COMPLEX_PARAM = new ComplexErrorParam(
            "StrVal",
            11,
            ImmutableList.<VdlUint32>of(new VdlUint32(22), new VdlUint32(33)));

    public static final VException COMPLEX_ERROR = VException.explicitMake(
            Errors.ERR_COMPLEX, "en", "test", "test", COMPLEX_PARAM, "secondParam", 3);

    private String lastAddedFortune;

    @Override
    public String get(VContext context, ServerCall call) throws VException {
        if (lastAddedFortune == null) {
            throw VException.make(Errors.ERR_NO_FORTUNES, context);
        }
        return lastAddedFortune;
    }

    @Override
    public void add(VContext context, ServerCall call, String fortune) throws VException {
        lastAddedFortune = fortune;
    }

    @Override
    public int streamingGet(VContext context, ServerCall call, Stream<String, Boolean> stream)
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
            throw VException.make(Errors.ERR_NO_FORTUNES, context);
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
        if (call.localEndpoint() == null || call.localEndpoint().isEmpty()) {
            throw new VException("Local endpoint is empty");
        }
        if (call.remoteEndpoint() == null || call.remoteEndpoint().isEmpty()) {
            throw new VException("Remote endpoint is empty");
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
        final GlobReply.Entry entry = new GlobReply.Entry(
                new MountEntry("helloworld", ImmutableList.<MountedServer>of(), false, false));
        response.writeValue(entry);
        final GlobReply.Error error = new GlobReply.Error(
                new GlobError("Hello, world!", new VException("Some error")));
        response.writeValue(error);
        response.close();
    }
}
