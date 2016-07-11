// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase;

import junit.framework.Assert;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.NoSuchElementException;

import io.v.syncbase.core.VError;
import io.v.syncbase.exception.SyncbaseException;
import io.v.syncbase.exception.SyncbaseRaceException;

import static com.google.common.truth.Truth.assertThat;
import static io.v.syncbase.exception.Exceptions.chainThrow;

public class ExceptionsTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /**
     * Make VError, using reflection to call private constructor.
     */
    private static VError newVError(String message, String goErrorId) throws
            IllegalAccessException, InvocationTargetException,
            InstantiationException, NoSuchMethodException {
        Constructor<VError> constructor = VError.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        VError e = constructor.newInstance();
        e.message = message;
        e.id = goErrorId;
        return e;
    }

    @Test
    public void exist() throws SyncbaseException, IllegalAccessException, InstantiationException,
            NoSuchMethodException, InvocationTargetException {
        VError cause = newVError("bar", VError.EXIST);

        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("while fooing got error bar: Exist");

        chainThrow("fooing", cause);
    }

    @Test
    public void noExist() throws SyncbaseException, IllegalAccessException, InstantiationException,
            NoSuchMethodException, InvocationTargetException {
        VError cause = newVError("bar", VError.NO_EXIST);

        thrown.expect(NoSuchElementException.class);
        thrown.expectMessage("while fooing got error bar: NoExist");

        chainThrow("fooing", cause);
    }

    @Test
    public void concurrentBatch() throws SyncbaseException, IllegalAccessException,
            InstantiationException,
            NoSuchMethodException, InvocationTargetException {
        VError cause = newVError("bar", VError.SYNCBASE_CONCURRENT_BATCH);

        thrown.expect(SyncbaseRaceException.class);
        thrown.expectMessage("while fooing got error bar: ConcurrentBatch");

        chainThrow("fooing", cause);
    }

    @Test
    public void allv23ErrorsFromGo() throws InvocationTargetException, NoSuchMethodException,
            InstantiationException, IllegalAccessException {
        // See https://godoc.org/v.io/v23/verror#pkg-variables
        String[] v23Errors = {
                "Unknown",
                "Internal",
                "NotImplemented",
                "EndOfFile",
                "BadArg",
                "BadState",
                "BadVersion",
                "Exist",
                "NoExist",
                "UnknownMethod",
                "UnknownSuffix",
                "NoExistOrNoAccess",
                "NoServers",
                "NoAccess",
                "NotTrusted",
                "Aborted",
                "BadProtocol",
                "Canceled",
                "Timeout",
        };
        for (String error : v23Errors) {
            String errorId = "v.io/v23/verror." + error;

            VError cause = newVError("bar", errorId);
            try {
                chainThrow("fooing", cause);
                Assert.fail("Expected assertion to be thrown");
            } catch (SyncbaseException e) {
                assertThat(e.getCause()).isNotNull();
                assertThat(e.getCause()).isInstanceOf(VError.class);
                assertThat(e.getMessage().contains(": " + error));
                assertThat(e.getMessage()).doesNotContain("unexpected error ID");
            } catch (RuntimeException e) {
                assertThat(e.getCause()).isNotNull();
                assertThat(e.getCause()).isInstanceOf(VError.class);
                assertThat(e.getMessage().contains(": " + error));
                assertThat(e.getMessage()).doesNotContain("unexpected error ID");
            }
        }
    }

    @Test
    public void allSyncbaseErrorsFromGo() throws InvocationTargetException, NoSuchMethodException,
            InstantiationException, IllegalAccessException {
        // See https://godoc.org/v.io/v23/verror#pkg-variables
        String[] syncbaseErrors = {
                "NotInDevMode",
                "InvalidName",
                "CorruptDatabase",
                "UnknownBatch",
                "NotBoundToBatch",
                "ReadOnlyBatch",
                "ConcurrentBatch",
                "BlobNotCommitted",
                "SyncgroupJoinFailed",
                "BadExecStreamHeader",
                "InvalidPermissionsChange",
                "UnauthorizedCreateId",
                "InferAppBlessingFailed",
                "InferUserBlessingFailed",
                "InferDefaultPermsFailed",
        };
        for (String error : syncbaseErrors) {
            String errorId = "v.io/v23/services/syncbase." + error;

            VError cause = newVError("bar", errorId);
            try {
                chainThrow("fooing", cause);
                Assert.fail("Expected assertion to be thrown");
            } catch (SyncbaseException e) {
                assertThat(e.getCause()).isNotNull();
                assertThat(e.getCause()).isInstanceOf(VError.class);
                assertThat(e.getMessage().contains(": " + error));
                assertThat(e.getMessage()).doesNotContain("unexpected error ID");
            } catch (RuntimeException e) {
                assertThat(e.getCause()).isNotNull();
                assertThat(e.getCause()).isInstanceOf(VError.class);
                assertThat(e.getMessage().contains(": " + error));
                assertThat(e.getMessage()).doesNotContain("unexpected error ID");
            }
        }
    }

}
