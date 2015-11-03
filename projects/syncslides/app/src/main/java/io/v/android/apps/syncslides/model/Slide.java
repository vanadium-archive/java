// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.syncslides.model;

import android.graphics.Bitmap;

/**
 * A slide.
 */
public interface Slide {
    /**
     * Returns a Bitmap of the slide thumbnail.
     */
    Bitmap getThumb();

    /**
     * Returns the raw thumbnail data.
     */
    byte[] getThumbData();

    /**
     * Returns a Bitmap of the slide image.
     */
    Bitmap getImage();

    /**
     * Returns the raw image data.
     */
    byte[] getImageData();

    /**
     * Returns the slide notes.
     */
    String getNotes();

    /**
     * Sets the slide notes.
     */
    void setNotes(String notes);
}
