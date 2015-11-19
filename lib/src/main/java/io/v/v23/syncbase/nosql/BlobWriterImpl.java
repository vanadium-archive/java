// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.syncbase.nosql;

import io.v.v23.context.VContext;
import io.v.v23.services.syncbase.nosql.BlobRef;
import io.v.v23.services.syncbase.nosql.DatabaseClient;
import io.v.v23.vdl.ClientSendStream;
import io.v.v23.verror.VException;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class BlobWriterImpl implements BlobWriter {
    private final DatabaseClient client;
    private final BlobRef ref;

    BlobWriterImpl(DatabaseClient client, BlobRef ref) {
        this.client = client;
        this.ref = ref;
    }

    @Override
    public BlobRef getRef() {
        return ref;
    }
    @Override
    public OutputStream stream(VContext ctx) throws VException {
        return new BufferedOutputStream(new BlobOutputStream(client.putBlob(ctx, ref)), 1 << 14);
    }
    @Override
    public void commit(VContext ctx) throws VException {
        client.commitBlob(ctx, ref);
    }
    @Override
    public long size(VContext ctx) throws VException {
        return client.getBlobSize(ctx, ref);
    }
    @Override
    public void delete(VContext ctx) throws VException {
        client.deleteBlob(ctx, ref);
    }

    private static class BlobOutputStream extends OutputStream {
        private final ClientSendStream<byte[], Void> stream;
        private boolean closed = false;

        BlobOutputStream(ClientSendStream<byte[], Void> stream) {
            this.stream = stream;
        }
        @Override
        public synchronized void close() throws IOException {
            if (closed) {
                return;
            }
            try {
                this.stream.finish();
                closed = true;
            } catch (VException e) {
                throw new IOException(e);
            }
        }
        @Override
        public void flush() throws IOException {}
        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            // TODO(spetrovic): support stream.send(b, off, len);
            byte[] copy = new byte[len];
            System.arraycopy(b, off, copy, 0, len);
            write(copy);
        }
        @Override
        public void write(byte[] b) throws IOException {
            try {
                stream.send(b);
            } catch (VException e) {
                throw new IOException(e);
            }
        }

        public void write(int b) throws IOException {
            write(new byte[] { (byte) b });
        }
    }
}
