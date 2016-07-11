// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase.exception;

import java.util.NoSuchElementException;
import java.util.concurrent.CancellationException;

import io.v.syncbase.Id;
import io.v.syncbase.core.VError;
import io.v.v23.verror.VException;
import io.v.v23.verror.VException.ActionCode;

import static io.v.v23.verror.VException.ActionCode.fromValue;

/**
 * Utility for exception chaining.
 */
public final class Exceptions {

    private Exceptions() {
    }

    private static String baseName(String v23ErrorId) {
        String[] tokens = v23ErrorId.split("\\.");
        int n = tokens.length;
        if (n < 1) {
            return v23ErrorId;
        }
        return tokens[n - 1];
    }

    private static void chainThrow(String message, String goErrorId, ActionCode action, Exception
            cause)
            throws SyncbaseException {
        if (goErrorId == null) {
            goErrorId = "null";
        }
        String fullMessage = message + ": " + baseName(goErrorId);
        switch (goErrorId) {
            case "v.io/v23/verror.NotImplemented":
                throw new UnsupportedOperationException(fullMessage, cause);

            case "v.io/v23/verror.EndOfFile":
                throw new SyncbaseEndOfFileException(fullMessage, cause);

            case "v.io/v23/verror.BadArg":
            case "v.io/v23/services/syncbase.InvalidName":
            case "v.io/v23/services/syncbase.NotBoundToBatch":
            case "v.io/v23/services/syncbase.ReadOnlyBatch":
                throw new IllegalArgumentException(fullMessage, cause);

            case "v.io/v23/verror.Exist":
            case "v.io/v23/services/syncbase.NotInDevMode":
            case "v.io/v23/services/syncbase.BlobNotCommitted":
            case "v.io/v23/services/syncbase.InvalidPermissionsChange":
            case "v.io/v23/verror.Aborted":
                throw new IllegalStateException(fullMessage, cause);

            case "v.io/v23/verror.NoExist":
                throw withCause(new NoSuchElementException(fullMessage), cause);

            case "v.io/v23/verror.Unknown":
            case "v.io/v23/verror.Internal":
            case "v.io/v23/verror.BadState":
            case "v.io/v23/verror.UnknownMethod":
            case "v.io/v23/verror.UnknownSuffix":
            case "v.io/v23/verror.BadProtocol":
            case "v.io/v23/services/syncbase.BadExecStreamHeader":
                throw new SyncbaseInternalException(fullMessage, cause);

            case "v.io/v23/verror.NoServers":
            case "v.io/v23/services/syncbase.SyncgroupJoinFailed":
                throw new SyncbaseNoServersException(fullMessage, cause);

            case "v.io/v23/verror.Canceled":
                throw withCause(new CancellationException(fullMessage), cause);

            case "v.io/v23/verror.Timeout":
                throw new SyncbaseRetryBackoffException(fullMessage, cause);

            case "v.io/v23/services/syncbase.CorruptDatabase":
                throw new SyncbaseRestartException(fullMessage, cause);

            case "v.io/v23/verror.BadVersion":
            case "v.io/v23/services/syncbase.ConcurrentBatch":
            case "v.io/v23/services/syncbase.UnknownBatch":
                throw new SyncbaseRaceException(fullMessage, cause);

            case "v.io/v23/verror.NoAccess":
            case "v.io/v23/verror.NotTrusted":
            case "v.io/v23/verror.NoExistOrNoAccess":
            case "v.io/v23/services/syncbase.UnauthorizedCreateId":
            case "v.io/v23/services/syncbase.InferAppBlessingFailed":
            case "v.io/v23/services/syncbase.InferUserBlessingFailed":
            case "v.io/v23/services/syncbase.InferDefaultPermsFailed":
                throw new SyncbaseSecurityException(fullMessage, cause);

            default:
                String fullerMessage = fullMessage + " (unexpected error ID " + goErrorId + ")";
                // See https://godoc.org/v.io/v23/verror#ActionCode
                switch (action) {
                    case RETRY_REFETCH:
                        throw new SyncbaseRetryRefetchException(fullerMessage, cause);
                    case RETRY_BACKOFF:
                        throw new SyncbaseRetryBackoffException(fullerMessage, cause);
                    case RETRY_CONNECTION:
                        throw new SyncbaseRetryConnectionException(fullerMessage, cause);
                    case NO_RETRY:
                    default:
                        throw new SyncbaseInternalException(fullerMessage, cause);
                }
        }
    }

    private static void chainThrow(String javaMessage, String goMessage, String v23ErrorId,
                                   ActionCode action, Exception cause) throws SyncbaseException {
        chainThrow("while " + javaMessage + " got error " + goMessage, v23ErrorId, action, cause);
    }

    public static void chainThrow(String javaMessage, VError cause) throws SyncbaseException {
        ActionCode action = fromValue((int) cause.actionCode);
        chainThrow(javaMessage, cause.message, cause.id, action, cause);
    }

    public static void chainThrow(String javaMessage, VException cause) throws SyncbaseException {
        chainThrow(javaMessage, cause.getMessage(), cause.getID(), cause.getAction(),
                cause);
    }

    public static void chainThrow(String doing, Id where, VError cause) throws SyncbaseException {
        chainThrow(doing + " " + where.getName(), cause);
    }

    public static void chainThrow(String doing, Id where, VException cause) throws
            SyncbaseException {
        chainThrow(doing + " " + where.getName(), cause);
    }

    public static void chainThrow(String doing, io.v.syncbase.core.Id where, VError cause) throws
            SyncbaseException {
        chainThrow(doing + " " + where.name, cause);
    }

    private static <T extends Exception> T withCause(T e, Exception cause) {
        e.initCause(cause);
        return e;
    }
}
