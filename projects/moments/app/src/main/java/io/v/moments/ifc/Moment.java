// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.moments.ifc;

import android.graphics.Bitmap;

import org.joda.time.DateTime;

import io.v.moments.lib.Id;

/**
 * A photo with ancillary information.
 */
public interface Moment extends HasId {
    /**
     * A unique moment ID valid for the life of the app.
     */
    Id getId();

    /**
     * Photos are stored as compressed byte arrays in numbered file names like a
     * camera would, e.g. img001.jpg, img002.jpg, etc.  The number used there is
     * the moments 'ordinal' number.
     *
     * This lets names have some correlation with creation order, makes file
     * lists easier to read in file managers, eases debugging relative to random
     * file names, etc.
     *
     * The ordinal is constant for this object, but not unique for life of app,
     * since if a moment is removed, its ordinal might be assigned to a new
     * moment.
     */
    int getOrdinal();

    /**
     * Some notion of who or what made the moment / took the photo.
     */
    String getAuthor();

    /**
     * More text describing the moment.
     */
    String getCaption();

    /**
     * Moment creation time.
     */
    DateTime getCreationTime();

    /**
     * Should the moment be advertised?
     */
    boolean shouldBeAdvertising();

    /**
     * Is the specified photo available?
     */
    boolean hasPhoto(Kind kind, Style style);

    /**
     * Get the specified photo.
     */
    Bitmap getPhoto(Kind kind, Style style);

    /**
     * Save the specified photo.
     *
     * The incoming byte array  comes from either the local camera or some
     * service on the net.  The storage location, compression level, clipping,
     * etc. are determined by the impl of the given style.
     */
    void setPhoto(Kind kind, Style style, byte[] data);

    /**
     * The different kinds of moments.
     */
    enum Kind {
        LOCAL, REMOTE
    }

    /**
     * The different styles of photos known to a moment.
     */
    enum Style {
        HUGE, FULL, THUMB
    }
}
