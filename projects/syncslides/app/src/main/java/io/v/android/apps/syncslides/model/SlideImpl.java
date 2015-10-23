// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.syncslides.model;

import android.graphics.Bitmap;

/**
 * Application impl of Slide.
 */
public class SlideImpl implements Slide {
    private final Bitmap mThumbnail;
    private String mNotes;

    public SlideImpl(Bitmap thumbnail, String notes) {
        mThumbnail = thumbnail;
        mNotes = notes;
    }

    @Override
    public Bitmap getImage() {
        return mThumbnail;
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
