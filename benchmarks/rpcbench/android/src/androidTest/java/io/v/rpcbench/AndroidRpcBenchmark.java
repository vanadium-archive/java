// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.rpcbench;

import android.os.Debug;
import android.test.AndroidTestCase;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Arrays;
import java.util.Iterator;

import io.v.android.v23.V;
import io.v.v23.VFutures;
import io.v.v23.context.VContext;
import io.v.v23.naming.Endpoint;
import io.v.v23.rpc.ListenSpec;
import io.v.v23.rpc.ReflectInvoker;
import io.v.v23.rpc.Server;
import io.v.v23.rpc.ServerCall;
import io.v.v23.security.VSecurity;
import io.v.v23.vdl.ServerStream;
import io.v.v23.verror.VException;
import io.v.v23.vom.BinaryDecoder;
import io.v.v23.vom.BinaryEncoder;
import io.v.v23.vom.ConversionException;
import io.v.v23.vom.VomUtil;

public class AndroidRpcBenchmark extends AndroidTestCase {

    public void testRunBenchmarks() throws VException {
        VContext baseContext = V.init(getContext());
        VContext listenSpec = V.withListenSpec(baseContext,
                new ListenSpec(new ListenSpec.Address("tcp", "127.0.0.1:0"), null, null));
        VContext serverContext = io.v.v23.V
                .withNewServer(listenSpec, "", new EchoServer() {
                    @Override
                    public byte[] echo(VContext ctx, ServerCall call, byte[] payload)
                            throws VException {
                        return payload;
                    }

                    @Override
                    public void echoStream(VContext ctx, ServerCall call,
                                           ServerStream<byte[], byte[]> stream)
                            throws VException {
                        Iterator<byte[]> byteIterator = stream.iterator();
                        while (byteIterator.hasNext()) {
                            byte[] payload = byteIterator.next();
                            stream.send(payload);
                        }
                        if (stream.error() != null) {
                            throw stream.error();
                        }
                    }
                }, VSecurity.newAllowEveryoneAuthorizer());
        Server echoServer = V.getServer(serverContext);

        Endpoint[] endpoints = echoServer.getStatus().getEndpoints();
        if (endpoints.length == 0) {
            throw new IllegalStateException("No endpoints for server");
        }
        EchoClient echoClient = EchoClientFactory.getEchoClient("/" + endpoints[0]);
        int payloadSize = 1;
        byte[] payload = new byte[payloadSize];
        for (int i = 0; i < payloadSize; i++) {
            payload[i] = (byte) (i & 0xFF);
        }
        double cma = 0;
        for (int i = 0; i < 100; i++) {
            VFutures.sync(echoClient.echo(baseContext, payload));
        }
        for (int i = 0; i < 1000; i++) {
            if (i == 999) { Debug.startMethodTracing(); }
            long start = System.nanoTime();
            VFutures.sync(echoClient.echo(baseContext, payload));
            long end = System.nanoTime();
            if (i == 999) { Debug.stopMethodTracing(); }
            long duration = end - start;
            if (i == 0) {
                cma = duration;
            } else if (i != 999) {
                cma += (duration - cma) / (i + 1);
            }
        }
        System.out.println("echo average time taken: " + (int) cma + "ns");
    }

    public void testSingleEncoder() throws IOException, ConversionException {
        int nreps = 1;
        PipedOutputStream outStream = new PipedOutputStream();
        PipedInputStream inStream = new PipedInputStream(outStream);
        BinaryEncoder encoder = new BinaryEncoder(outStream);
        BinaryDecoder decoder = new BinaryDecoder(new BufferedInputStream(inStream));
        byte[] payload = new byte[100];
        for (int i = 0; i < payload.length; i++) {
            payload[i] = (byte) (i & 0xFF);
        }
        double cma = 0;
        for (int i = 0; i < nreps; i++) {
            long start = System.nanoTime();
            encoder.encodeValue(byte[].class, payload);
            byte[] decoded = (byte[]) decoder.decodeValue(byte[].class);
            long end = System.nanoTime();
            if (!Arrays.equals(payload, decoded)) {
                throw new IllegalStateException("unexpected output from decoder");
            }
            long duration = end - start;
            if (i == 0) {
                cma = duration;
            } else {
                cma += (duration - cma) / (i + 1);
            }
        }
        System.out.println("single encoder average time taken: " + (int) cma + "ns");
    }

    public void testNewEncoderEachTime() throws VException {
        int nreps = 1;
        byte[] payload = new byte[] { 0x01 };
        double cma = 0;
        for (int i = 0; i < nreps; i++) {
            long start = System.nanoTime();
            byte[] result = VomUtil.encode(payload, byte[].class);
            byte[] decoded = (byte[]) VomUtil.decode(result, byte[].class);
            long end = System.nanoTime();
            if (!Arrays.equals(payload, decoded)) {
                throw new IllegalStateException("unexpected output from decoder");
            }
            long duration = end - start;
            if (i == 0) {
                cma = duration;
            } else {
                cma += (duration - cma) / (i + 1);
            }
        }
        System.out.println("new encoder average time taken: " + (int) cma + "ns");
    }
}
