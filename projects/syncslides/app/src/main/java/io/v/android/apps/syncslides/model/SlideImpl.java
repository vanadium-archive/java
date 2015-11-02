// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.syncslides.model;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

/**
 * Application impl of Slide.
 */
public class SlideImpl implements Slide {
    private final byte[] mThumbnail;
    private final byte[] mImage;
    private String mNotes;

    public SlideImpl(byte[] thumbnail, byte[] image, String notes) {
        mThumbnail = thumbnail;
        mImage = image;
        mNotes = notes;
    }

    @Override
    public Bitmap getThumb() {
        return BitmapFactory.decodeByteArray(
                mThumbnail, 0 /* offset */, mThumbnail.length);
    }

    @Override
    public Bitmap getImage() {
        return BitmapFactory.decodeByteArray(
                mImage, 0 /* offset */, mImage.length);
    }

    @Override
    public String getNotes() {
        return mNotes;
    }

    @Override
    public void setNotes(String notes) {
        mNotes = notes;
    }
}
