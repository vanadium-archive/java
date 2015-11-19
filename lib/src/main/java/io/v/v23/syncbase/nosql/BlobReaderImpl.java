// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.syncbase.nosql;

import io.v.v23.VIterable;
import io.v.v23.context.VContext;
import io.v.v23.services.syncbase.nosql.BlobFetchStatus;
import io.v.v23.services.syncbase.nosql.BlobRef;
import io.v.v23.services.syncbase.nosql.DatabaseClient;
import io.v.v23.vdl.ClientRecvStream;
import io.v.v23.vdl.VdlUint64;
import io.v.v23.verror.VException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.NoSuchElementException;

class BlobReaderImpl implements BlobReader {
    private final DatabaseClient client;
    private final BlobRef ref;

    BlobReaderImpl(DatabaseClient client, BlobRef ref) {
        this.client = client;
        this.ref = ref;
    }

    @Override
    public BlobRef getRef() {
        return ref;
    }
    @Override
    public InputStream stream(VContext ctx, long offset) throws VException {
        ClientRecvStream<byte[], Void> stream = client.getBlob(ctx, ref, offset);
        return new BlobInputStream(stream);
    }
    @Override
    public VIterable<BlobFetchStatus> prefetch(VContext ctx, long priority) throws VException {
        return client.fetchBlob(ctx, ref, new VdlUint64(priority));
    }

    @Override
    public long size(VContext ctx) throws VException {
        return client.getBlobSize(ctx, ref);
    }
    @Override
    public void delete(VContext ctx) throws VException {
        client.deleteBlob(ctx, ref);
    }
    @Override
    public void pin(VContext ctx) throws VException {
        client.pinBlob(ctx, ref);
    }
    @Override
    public void unpin(VContext ctx) throws VException {
        client.unpinBlob(ctx, ref);
    }
    @Override
    public void keep(VContext ctx, long rank) throws VException {
        client.keepBlob(ctx, ref, new VdlUint64(rank));
    }

    private static class BlobInputStream extends InputStream {
        private final ClientRecvStream<byte[], Void> stream;
        private final Iterator<byte[]> streamIt;
        private boolean closed = false;
        private byte[] lastRecv = null;
        private int lastRecvRemaining = 0;
        private boolean eof = false;

        BlobInputStream(ClientRecvStream<byte[], Void> stream) {
            this.stream = stream;
            this.streamIt = stream.iterator();
        }

        @Override
        public int available() {
            return 0;
        }
        @Override
        public synchronized void close() throws IOException {
            if (closed) {
                return;
            }
            try {
                stream.finish();
                closed = true;
            } catch (VException e) {
                throw new IOException(e);
            }
        }
        @Override
        public boolean markSupported() {
            return false;
        }
        @Override
        public synchronized int read() throws IOException {
            byte[] b = new byte[1];
            if (read(b) == -1) {
                return -1;
            }
            return b[0];
        }
        @Override
        public synchronized int read(byte[] b, int offset, int len) throws IOException {
            if (closed) {
                throw new IOException("Stream closed");
            }
            if (eof) {
                return -1;
            }
            if (b == null) {
                throw new NullPointerException();
            }
            if (offset < 0 || len < 0 || len > (b.length - offset)) {
                throw new IndexOutOfBoundsException();
            }
            if (len == 0) {
                return 0;
            }
            int need = len;
            while (need > 0) {
                if (lastRecvRemaining <= 0) {
                    try {
                        lastRecv = streamIt.next();
                        lastRecvRemaining = lastRecv.length;
                    } catch (NoSuchElementException e) {
                        eof = true;
                        break;
                    }
                    if (stream.error() != null) {
                        throw new IOException(stream.error());
                    }
                }
                int copyLen = len > lastRecvRemaining ? lastRecvRemaining : len;
                System.arraycopy(lastRecv,
                        lastRecv.length - lastRecvRemaining, b, offset + len - need, copyLen);
                lastRecvRemaining -= copyLen;
                need -= copyLen;
            }
            if (need == len) {
                return -1;
            }
            return len - need;
        }
        @Override
        public synchronized int read(byte[] b) throws IOException {
            return read(b, 0, b.length);
        }
    }
}
