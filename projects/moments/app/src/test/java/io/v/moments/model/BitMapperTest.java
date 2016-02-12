// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.moments.model;

import android.graphics.Point;
import android.os.Handler;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;

import io.v.moments.ifc.Moment;
import io.v.moments.lib.FileUtil;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class BitMapperTest {
    static final File SCRATCH_DIR = new File("/tmp/MomentFactoryTestScratch");
    static final Point DISPLAY_SIZE = new Point(2, 3);
    static final int THUMB_WIDTH = 33;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Mock
    Handler mHandler;

    BitMapper mBitMapper;

    @Before
    public void setup() throws Exception {
        if (SCRATCH_DIR.exists()) {
            FileUtil.rmMinusF(SCRATCH_DIR);
        }
        if (!SCRATCH_DIR.mkdirs()) {
            throw new IllegalStateException(
                    "Failed to create directory " +
                            SCRATCH_DIR.getAbsolutePath());
        }
        mBitMapper = new BitMapper(
                SCRATCH_DIR, mHandler, DISPLAY_SIZE, THUMB_WIDTH);
    }

    @After
    public void teardown() throws Exception {
        if (SCRATCH_DIR.exists()) {
            FileUtil.rmMinusF(SCRATCH_DIR);
        }
    }

    @Test
    public void makeFileName() {
        assertEquals(
                SCRATCH_DIR + "/REMOTE_THUMB_0011.jpg",
                mBitMapper.makeFileName(
                        11, Moment.Kind.REMOTE, Moment.Style.THUMB).toString());
        assertEquals(
                SCRATCH_DIR + "/LOCAL_THUMB_0022.jpg",
                mBitMapper.makeFileName(
                        22, Moment.Kind.LOCAL, Moment.Style.THUMB).toString());
        assertEquals(
                SCRATCH_DIR + "/LOCAL_FULL_0033.jpg",
                mBitMapper.makeFileName(
                        33, Moment.Kind.LOCAL, Moment.Style.FULL).toString());
    }

    @Test
    public void makeCameraPhotoFile() {
        assertEquals(
                SCRATCH_DIR + "/camera_0033.jpg",
                mBitMapper.makeCameraPhotoFile(33).toString());
    }
}
