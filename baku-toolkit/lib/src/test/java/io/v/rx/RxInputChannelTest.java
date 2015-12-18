// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.rx;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import io.v.baku.toolkit.RobolectricTestCase;
import io.v.v23.InputChannel;
import io.v.v23.verror.EndOfFileException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest(EndOfFileException.class)
public class RxInputChannelTest {
    @SuppressWarnings("unchecked")
    @Test
    public void test() {
        final InputChannel<Integer> mockInputChannel = mock(InputChannel.class);
        when(mockInputChannel.recv()).thenReturn(
                Futures.immediateFuture(1),
                Futures.immediateFuture(2),
                Futures.immediateFailedFuture(PowerMockito.mock(EndOfFileException.class)));
        assertEquals(ImmutableList.of(1, 2), RobolectricTestCase.first(
                RxInputChannel.wrap(mockInputChannel).autoConnect().toList()));
    }
}
