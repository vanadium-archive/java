// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.syncbase.nosql;

import com.google.common.util.concurrent.ListenableFuture;

import io.v.v23.InputChannel;
import io.v.v23.context.VContext;
import io.v.v23.services.syncbase.nosql.BlobFetchStatus;
import io.v.v23.services.syncbase.nosql.BlobRef;
import io.v.v23.services.syncbase.nosql.DatabaseClient;
import io.v.v23.vdl.ClientRecvStream;
import io.v.v23.vdl.VdlUint64;
import io.v.v23.verror.EndOfFileException;
import io.v.v23.verror.VException;

import java.io.IOException;
import java.io.InputStream;

import static io.v.v23.VFutures.sync;

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
    public InputStream stream(VContext ctx, long offset) {
        return new BlobInputStream(client.getBlob(ctx, ref, offset));
    }
    @Override
    public InputChannel<BlobFetchStatus> prefetch(VContext ctx, long priority) {
        return client.fetchBlob(ctx, ref, new VdlUint64(priority));
    }

    @Override
    public ListenableFuture<Long> size(VContext ctx) {
        return client.getBlobSize(ctx, ref);
    }
    @Override
    public ListenableFuture<Void> delete(VContext ctx) {
        return client.deleteBlob(ctx, ref);
    }
    @Override
    public ListenableFuture<Void> pin(VContext ctx) {
        return client.pinBlob(ctx, ref);
    }
    @Override
    public ListenableFuture<Void> unpin(VContext ctx) {
        return client.unpinBlob(ctx, ref);
    }
    @Override
    public ListenableFuture<Void> keep(VContext ctx, long rank) {
        return client.keepBlob(ctx, ref, new VdlUint64(rank));
    }

    private static class BlobInputStream extends InputStream {
        private final ClientRecvStream<byte[], Void> stream;
        private boolean closed = false;
        private byte[] lastRecv = null;
        private int lastRecvRemaining = 0;
        private boolean eof = false;

        BlobInputStream(ClientRecvStream<byte[], Void> stream) {
            this.stream = stream;
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
                sync(stream.finish());
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
                        lastRecv = sync(stream.recv());
                        lastRecvRemaining = lastRecv.length;
                    } catch (EndOfFileException e) {
                        eof = true;
                        break;
                    } catch (VException e) {
                        throw new IOException(e);
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
